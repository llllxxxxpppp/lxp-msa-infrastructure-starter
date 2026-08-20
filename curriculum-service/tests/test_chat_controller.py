"""사용자 기반 채팅과 세션 초기화 API 테스트."""

import json
from typing import Annotated
from unittest import TestCase
from unittest.mock import AsyncMock, Mock

from fastapi import FastAPI
from fastapi.testclient import TestClient
from langchain_core.messages import AIMessage, AnyMessage, HumanMessage
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from typing_extensions import TypedDict

from app.api.chat_controller import ChatController
from app.services.conversation_session_service import ConversationSessionService


def _result(message: str) -> dict:
    return {
        "messages": [AIMessage(content=message)],
        "status": "interviewing",
        "user_profile": {},
        "missing_info": [],
    }


def _client(session_service) -> TestClient:
    controller = ChatController(
        session_service=session_service,
        ollama_model="test-model",
        ollama_base_url="http://ollama.test",
        stream_chunk_delay_seconds=0,
    )
    app = FastAPI()
    app.include_router(controller.router)
    return TestClient(app)


def _sse_events(response) -> list[tuple[str, object]]:
    events = []
    for block in response.text.strip().split("\n\n"):
        lines = block.splitlines()
        event = next(
            line.removeprefix("event: ")
            for line in lines
            if line.startswith("event: ")
        )
        data = next(
            line.removeprefix("data: ")
            for line in lines
            if line.startswith("data: ")
        )
        events.append((event, json.loads(data)))
    return events


class ChatControllerApiTest(TestCase):
    def setUp(self) -> None:
        self.session_service = Mock()
        self.session_service.chat = AsyncMock(return_value=_result("응답"))
        self.session_service.delete = AsyncMock()
        self.client = _client(self.session_service)

    def test_chat_uses_header_user_and_ignores_body_thread_id(self) -> None:
        response = self.client.post(
            "/api/curriculum/chat",
            headers={"X-User-Id": " 7 "},
            json={"thread_id": "다른 사용자", "message": "안녕하세요"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertNotIn("thread_id", response.json())
        self.assertEqual(response.json()["message"], "응답")
        call = self.session_service.chat.await_args
        self.assertEqual(call.args[0], 7)
        self.assertEqual(call.args[1]["messages"][0].content, "안녕하세요")

    def test_chat_requires_message(self) -> None:
        response = self.client.post(
            "/api/curriculum/chat",
            headers={"X-User-Id": "1"},
            json={"thread_id": "1"},
        )

        self.assertEqual(response.status_code, 422)
        self.session_service.chat.assert_not_awaited()

    def test_chat_stream_returns_metadata_deltas_and_done(self) -> None:
        response = self.client.post(
            "/api/curriculum/chat/stream",
            headers={"X-User-Id": "7"},
            json={"message": "안녕하세요"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.headers["content-type"].startswith("text/event-stream"))
        events = _sse_events(response)
        self.assertEqual(events[0], ("start", {}))
        self.assertEqual(events[1][0], "metadata")
        self.assertNotIn("message", events[1][1])
        self.assertEqual(events[1][1]["status"], "interviewing")
        self.assertEqual(
            "".join(data["content"] for event, data in events if event == "delta"),
            "응답",
        )
        self.assertEqual(events[-1], ("done", {}))

    def test_chat_stream_requires_authentication_and_message(self) -> None:
        unauthenticated = self.client.post(
            "/api/curriculum/chat/stream",
            json={"message": "안녕하세요"},
        )
        invalid_request = self.client.post(
            "/api/curriculum/chat/stream",
            headers={"X-User-Id": "7"},
            json={},
        )

        self.assertEqual(unauthenticated.status_code, 401)
        self.assertEqual(invalid_request.status_code, 422)
        self.session_service.chat.assert_not_awaited()

    def test_chat_stream_only_emits_review_prompt_with_curriculum(self) -> None:
        prompt = "이 커리큘럼이 괜찮으신가요? 바꾸고 싶은 부분이 있다면 말씀해 주세요."
        curriculum = {
            "summary": "학습 방향",
            "steps": [
                {
                    "stage": "입문",
                    "course_id": 1,
                    "title": "입문 강좌",
                    "duration_minutes": 30,
                    "reason": "기초 학습",
                }
            ],
        }
        self.session_service.chat.return_value = {
            **_result(f"학습 방향\n\n- 입문: 입문 강좌\n\n{prompt}"),
            "status": "reviewing",
            "draft_curriculum": curriculum,
        }

        response = self.client.post(
            "/api/curriculum/chat/stream",
            headers={"X-User-Id": "7"},
            json={"message": "추천해 주세요"},
        )

        events = _sse_events(response)
        metadata = next(data for event, data in events if event == "metadata")
        text = "".join(data["content"] for event, data in events if event == "delta")
        self.assertEqual(metadata["curriculum"], curriculum)
        self.assertEqual(text, prompt)

    def test_chat_stream_converts_workflow_failure_to_error_event(self) -> None:
        self.session_service.chat.side_effect = RuntimeError("연결 실패")

        response = self.client.post(
            "/api/curriculum/chat/stream",
            headers={"X-User-Id": "7"},
            json={"message": "안녕하세요"},
        )

        events = _sse_events(response)
        self.assertEqual(events[0], ("start", {}))
        self.assertEqual(events[1][0], "error")
        self.assertIn("Ollama 모델 호출에 실패했습니다.", events[1][1]["message"])
        self.assertEqual(len(events), 2)

    def test_invalid_user_id_returns_401_without_calling_session(self) -> None:
        for user_id in (None, "", "0", "invalid"):
            with self.subTest(user_id=user_id):
                self.session_service.chat.reset_mock()
                headers = {}
                if user_id is not None:
                    headers["X-User-Id"] = user_id

                response = self.client.post(
                    "/api/curriculum/chat",
                    headers=headers,
                    json={"message": "안녕하세요"},
                )

                self.assertEqual(response.status_code, 401)
                self.session_service.chat.assert_not_awaited()

    def test_delete_session_is_idempotent_and_returns_empty_204(self) -> None:
        for _ in range(2):
            response = self.client.delete(
                "/api/curriculum/chat/session",
                headers={"X-User-Id": "1"},
            )

            self.assertEqual(response.status_code, 204)
            self.assertEqual(response.content, b"")

        self.assertEqual(self.session_service.delete.await_count, 2)
        self.session_service.delete.assert_awaited_with(1)

    def test_delete_session_requires_authenticated_user(self) -> None:
        response = self.client.delete("/api/curriculum/chat/session")

        self.assertEqual(response.status_code, 401)
        self.session_service.delete.assert_not_awaited()


class ConversationState(TypedDict):
    messages: Annotated[list[AnyMessage], add_messages]


class FakeClock:
    def __init__(self) -> None:
        self.now = 0.0

    def __call__(self) -> float:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += seconds


def _stateful_session_service(clock: FakeClock) -> ConversationSessionService:
    async def respond(state: ConversationState) -> dict:
        turn_count = sum(
            isinstance(message, HumanMessage) for message in state["messages"]
        )
        return {"messages": [AIMessage(content=f"{turn_count}번째 메시지")]}

    graph_builder = StateGraph(ConversationState)
    graph_builder.add_node("respond", respond)
    graph_builder.add_edge(START, "respond")
    graph_builder.add_edge("respond", END)
    checkpointer = InMemorySaver()
    graph = graph_builder.compile(checkpointer=checkpointer)
    return ConversationSessionService(
        graph=graph,
        checkpointer=checkpointer,
        timeout_seconds=30,
        cleanup_interval_seconds=10,
        clock=clock,
    )


class ChatSessionIntegrationTest(TestCase):
    def setUp(self) -> None:
        self.clock = FakeClock()
        self.client = _client(_stateful_session_service(self.clock))

    def _chat(self, user_id: int) -> str:
        response = self.client.post(
            "/api/curriculum/chat",
            headers={"X-User-Id": str(user_id)},
            json={"message": "메시지"},
        )
        self.assertEqual(response.status_code, 200)
        return response.json()["message"]

    def test_conversation_state_is_kept_per_user(self) -> None:
        self.assertEqual(self._chat(1), "1번째 메시지")
        self.assertEqual(self._chat(1), "2번째 메시지")
        self.assertEqual(self._chat(2), "1번째 메시지")

    def test_explicit_reset_starts_new_conversation(self) -> None:
        self.assertEqual(self._chat(1), "1번째 메시지")

        response = self.client.delete(
            "/api/curriculum/chat/session",
            headers={"X-User-Id": "1"},
        )
        repeated_response = self.client.delete(
            "/api/curriculum/chat/session",
            headers={"X-User-Id": "1"},
        )

        self.assertEqual(response.status_code, 204)
        self.assertEqual(repeated_response.status_code, 204)
        self.assertEqual(self._chat(1), "1번째 메시지")

    def test_expired_session_starts_new_conversation_without_error(self) -> None:
        self.assertEqual(self._chat(1), "1번째 메시지")
        self.clock.advance(30)

        self.assertEqual(self._chat(1), "1번째 메시지")
