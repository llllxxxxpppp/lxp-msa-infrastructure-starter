"""사용자별 인메모리 대화 세션 수명 주기 테스트."""

import asyncio
from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock

from app.services.conversation_session_service import ConversationSessionService


class FakeClock:
    def __init__(self) -> None:
        self.now = 0.0

    def __call__(self) -> float:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += seconds


class ConversationSessionServiceTest(IsolatedAsyncioTestCase):
    def setUp(self) -> None:
        self.clock = FakeClock()
        self.graph = AsyncMock()
        self.graph.ainvoke.return_value = {"status": "interviewing"}
        self.checkpointer = AsyncMock()
        self.service = ConversationSessionService(
            graph=self.graph,
            checkpointer=self.checkpointer,
            timeout_seconds=30,
            cleanup_interval_seconds=10,
            clock=self.clock,
        )

    async def test_uses_normalized_user_id_as_thread_id(self) -> None:
        result = await self.service.chat(7, {"messages": ["안녕하세요"]})

        self.assertEqual(result, {"status": "interviewing"})
        self.graph.ainvoke.assert_awaited_once_with(
            {"messages": ["안녕하세요"]},
            config={"configurable": {"thread_id": "7"}},
        )

    async def test_keeps_session_before_timeout_and_expires_at_boundary(self) -> None:
        await self.service.chat(1, {"messages": ["첫 메시지"]})

        self.clock.advance(29.9)
        await self.service.cleanup_expired()
        self.checkpointer.adelete_thread.assert_not_awaited()

        self.clock.advance(0.1)
        await self.service.cleanup_expired()
        self.checkpointer.adelete_thread.assert_awaited_once_with("1")

    async def test_request_entry_removes_expired_checkpoints(self) -> None:
        await self.service.chat(1, {"messages": ["첫 메시지"]})
        self.clock.advance(30)

        await self.service.chat(1, {"messages": ["새 메시지"]})

        self.checkpointer.adelete_thread.assert_awaited_once_with("1")
        self.assertEqual(self.graph.ainvoke.await_count, 2)

    async def test_periodic_cleanup_removes_expired_session(self) -> None:
        sleep = AsyncMock(side_effect=[None, asyncio.CancelledError()])
        service = ConversationSessionService(
            graph=self.graph,
            checkpointer=self.checkpointer,
            timeout_seconds=30,
            cleanup_interval_seconds=10,
            clock=self.clock,
            sleep=sleep,
        )
        await service.chat(2, {"messages": ["다른 메시지"]})
        self.clock.advance(30)

        with self.assertRaises(asyncio.CancelledError):
            await service.run_cleanup()

        sleep.assert_any_await(10)
        self.checkpointer.adelete_thread.assert_awaited_once_with("2")

    async def test_delete_only_removes_requested_user_session(self) -> None:
        await self.service.chat(1, {"messages": ["사용자 1"]})
        await self.service.chat(2, {"messages": ["사용자 2"]})

        await self.service.delete(1)
        await self.service.chat(2, {"messages": ["사용자 2의 다음 메시지"]})

        deleted_thread_ids = [
            call.args[0]
            for call in self.checkpointer.adelete_thread.await_args_list
        ]
        self.assertEqual(deleted_thread_ids, ["1"])
        self.assertEqual(
            [
                call.kwargs["config"]["configurable"]["thread_id"]
                for call in self.graph.ainvoke.await_args_list
            ],
            ["1", "2", "2"],
        )

    async def test_same_user_chat_requests_are_serialized(self) -> None:
        first_started = asyncio.Event()
        release_first = asyncio.Event()
        call_count = 0

        async def invoke(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                first_started.set()
                await release_first.wait()
            return {"call_count": call_count}

        self.graph.ainvoke.side_effect = invoke
        first = asyncio.create_task(self.service.chat(1, {"message": "첫 요청"}))
        await first_started.wait()
        second = asyncio.create_task(self.service.chat(1, {"message": "두 번째 요청"}))
        await asyncio.sleep(0)

        self.assertEqual(call_count, 1)
        release_first.set()
        await asyncio.gather(first, second)
        self.assertEqual(call_count, 2)

    async def test_different_users_can_run_in_parallel(self) -> None:
        active_users: set[str] = set()
        both_started = asyncio.Event()
        release = asyncio.Event()

        async def invoke(input_state, config):
            thread_id = config["configurable"]["thread_id"]
            active_users.add(thread_id)
            if len(active_users) == 2:
                both_started.set()
            await release.wait()
            return input_state

        self.graph.ainvoke.side_effect = invoke
        first = asyncio.create_task(self.service.chat(1, {"message": "사용자 1"}))
        second = asyncio.create_task(self.service.chat(2, {"message": "사용자 2"}))

        await asyncio.wait_for(both_started.wait(), timeout=1)
        self.assertEqual(active_users, {"1", "2"})
        release.set()
        await asyncio.gather(first, second)

    async def test_cleanup_does_not_expire_running_session(self) -> None:
        started = asyncio.Event()
        release = asyncio.Event()

        async def invoke(*args, **kwargs):
            started.set()
            await release.wait()
            return {"status": "interviewing"}

        self.graph.ainvoke.side_effect = invoke
        chat = asyncio.create_task(self.service.chat(1, {"message": "처리 중"}))
        await started.wait()
        self.clock.advance(300)

        await asyncio.wait_for(self.service.cleanup_expired(), timeout=1)

        self.checkpointer.adelete_thread.assert_not_awaited()
        release.set()
        await chat
        await self.service.cleanup_expired()
        self.checkpointer.adelete_thread.assert_not_awaited()

        self.clock.advance(30)
        await self.service.cleanup_expired()
        self.checkpointer.adelete_thread.assert_awaited_once_with("1")

    async def test_delete_waits_for_same_user_chat(self) -> None:
        started = asyncio.Event()
        release = asyncio.Event()

        async def invoke(*args, **kwargs):
            started.set()
            await release.wait()
            return {"status": "interviewing"}

        self.graph.ainvoke.side_effect = invoke
        chat = asyncio.create_task(self.service.chat(1, {"message": "처리 중"}))
        await started.wait()
        delete = asyncio.create_task(self.service.delete(1))
        await asyncio.sleep(0)

        self.checkpointer.adelete_thread.assert_not_awaited()
        release.set()
        await asyncio.gather(chat, delete)
        self.checkpointer.adelete_thread.assert_awaited_once_with("1")
