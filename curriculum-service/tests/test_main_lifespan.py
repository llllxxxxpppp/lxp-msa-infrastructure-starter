"""기동·종료 순서 테스트."""

import asyncio

from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock, Mock, call

from app.main import build_lifespan


def _dependencies() -> tuple[Mock, Mock, Mock, Mock, asyncio.Event]:
    recorder = Mock()
    recorder.consumer.declare = AsyncMock()
    recorder.consumer.start = AsyncMock()
    recorder.consumer.stop = AsyncMock()
    cleanup_finished = asyncio.Event()

    async def run_cleanup() -> None:
        try:
            await asyncio.Event().wait()
        finally:
            cleanup_finished.set()

    recorder.session_service.run_cleanup = AsyncMock(side_effect=run_cleanup)
    return (
        recorder,
        recorder.course_service,
        recorder.consumer,
        recorder.session_service,
        cleanup_finished,
    )


class LifespanTest(IsolatedAsyncioTestCase):
    async def test_startup_declares_before_loading_and_consumes_last(self) -> None:
        recorder, course_service, consumer, session_service, _ = _dependencies()
        lifespan = build_lifespan(course_service, consumer, session_service)

        async with lifespan(Mock()):
            await asyncio.sleep(0)
            self.assertEqual(
                recorder.mock_calls[:3],
                [
                    call.consumer.declare(),
                    call.course_service.load_all_courses(),
                    call.consumer.start(),
                ],
            )
            session_service.run_cleanup.assert_awaited_once_with()

    async def test_shutdown_stops_consumer(self) -> None:
        recorder, course_service, consumer, session_service, cleanup_finished = (
            _dependencies()
        )
        lifespan = build_lifespan(course_service, consumer, session_service)

        async with lifespan(Mock()):
            await asyncio.sleep(0)

        self.assertTrue(cleanup_finished.is_set())
        self.assertEqual(recorder.mock_calls[-1], call.consumer.stop())

    async def test_consumer_is_stopped_when_startup_fails(self) -> None:
        recorder, course_service, consumer, session_service, _ = _dependencies()
        course_service.load_all_courses.side_effect = ConnectionError("적재 실패")
        lifespan = build_lifespan(course_service, consumer, session_service)

        with self.assertRaises(ConnectionError):
            async with lifespan(Mock()):
                pass

        consumer.start.assert_not_awaited()
        session_service.run_cleanup.assert_not_awaited()
        consumer.stop.assert_awaited_once()
