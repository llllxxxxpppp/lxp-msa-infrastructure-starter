"""기동·종료 순서 테스트."""

from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock, Mock, call

from app.main import build_lifespan


def _dependencies() -> tuple[Mock, Mock, Mock]:
    recorder = Mock()
    recorder.consumer.declare = AsyncMock()
    recorder.consumer.start = AsyncMock()
    recorder.consumer.stop = AsyncMock()
    return recorder, recorder.course_service, recorder.consumer


class LifespanTest(IsolatedAsyncioTestCase):
    async def test_startup_declares_before_loading_and_consumes_last(self) -> None:
        recorder, course_service, consumer = _dependencies()
        lifespan = build_lifespan(course_service, consumer)

        async with lifespan(Mock()):
            self.assertEqual(
                recorder.mock_calls,
                [
                    call.consumer.declare(),
                    call.course_service.load_all_courses(),
                    call.consumer.start(),
                ],
            )

    async def test_shutdown_stops_consumer(self) -> None:
        recorder, course_service, consumer = _dependencies()
        lifespan = build_lifespan(course_service, consumer)

        async with lifespan(Mock()):
            pass

        self.assertEqual(recorder.mock_calls[-1], call.consumer.stop())

    async def test_consumer_is_stopped_when_startup_fails(self) -> None:
        recorder, course_service, consumer = _dependencies()
        course_service.load_all_courses.side_effect = ConnectionError("적재 실패")
        lifespan = build_lifespan(course_service, consumer)

        with self.assertRaises(ConnectionError):
            async with lifespan(Mock()):
                pass

        consumer.start.assert_not_awaited()
        consumer.stop.assert_awaited_once()
