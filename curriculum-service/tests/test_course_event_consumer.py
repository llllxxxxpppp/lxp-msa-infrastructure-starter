"""강좌 변경 이벤트 소비자 테스트."""

import json
from unittest import IsolatedAsyncioTestCase
from unittest.mock import AsyncMock, Mock

from pydantic import ValidationError

from app.messaging.course_event_consumer import (
    DELETED_ROUTING_KEY,
    PUBLISHED_ROUTING_KEY,
    UNPUBLISHED_ROUTING_KEY,
    CourseEvent,
    CourseEventConsumer,
)


EVENT_BODY = {
    "eventId": "3f9a0f4c-0f4b-4d3a-9c1a-2b6f0d5e7a11",
    "occurredAt": "2026-08-17T10:00:00+09:00",
    "courseId": 30017,
}


def _consumer(course_service: Mock) -> CourseEventConsumer:
    return CourseEventConsumer(
        course_service=course_service,
        host="localhost",
        port=5672,
        username="admin",
        password="admin",
        attempts=3,
        delay_seconds=0,
    )


def _message(routing_key: str, body: dict | str | None = None) -> Mock:
    message = Mock()
    message.routing_key = routing_key
    raw = body if isinstance(body, str) else json.dumps(body or EVENT_BODY)
    message.body = raw.encode("utf-8")
    message.ack = AsyncMock()
    message.reject = AsyncMock()
    return message


class CourseEventTest(IsolatedAsyncioTestCase):
    def test_parses_publisher_payload(self) -> None:
        event = CourseEvent.model_validate(EVENT_BODY)

        self.assertEqual(event.course_id, 30017)
        self.assertEqual(str(event.event_id), EVENT_BODY["eventId"])

    def test_ignores_unknown_fields(self) -> None:
        event = CourseEvent.model_validate({**EVENT_BODY, "title": "새 필드"})

        self.assertEqual(event.course_id, 30017)

    def test_rejects_missing_course_id(self) -> None:
        payload = {key: value for key, value in EVENT_BODY.items() if key != "courseId"}

        with self.assertRaises(ValidationError):
            CourseEvent.model_validate(payload)


class CourseEventDispatchTest(IsolatedAsyncioTestCase):
    def setUp(self) -> None:
        self.course_service = Mock()
        self.consumer = _consumer(self.course_service)

    async def test_published_updates_course_and_acks(self) -> None:
        message = _message(PUBLISHED_ROUTING_KEY)

        await self.consumer._on_message(message)

        self.course_service.update_course.assert_called_once_with(30017)
        message.ack.assert_awaited_once()
        message.reject.assert_not_awaited()

    async def test_published_acks_when_course_is_gone(self) -> None:
        self.course_service.update_course.return_value = None
        message = _message(PUBLISHED_ROUTING_KEY)

        with self.assertLogs("app.messaging.course_event_consumer", level="INFO") as logs:
            await self.consumer._on_message(message)

        self.assertIn("건너뜁니다", logs.output[-1])
        message.ack.assert_awaited_once()
        message.reject.assert_not_awaited()

    async def test_unpublished_removes_course(self) -> None:
        message = _message(UNPUBLISHED_ROUTING_KEY)

        await self.consumer._on_message(message)

        self.course_service.remove_course.assert_called_once_with(30017)
        self.course_service.update_course.assert_not_called()
        message.ack.assert_awaited_once()

    async def test_deleted_removes_course(self) -> None:
        message = _message(DELETED_ROUTING_KEY)

        await self.consumer._on_message(message)

        self.course_service.remove_course.assert_called_once_with(30017)
        message.ack.assert_awaited_once()

    async def test_unknown_routing_key_touches_nothing(self) -> None:
        message = _message("course.something-else")

        await self.consumer._on_message(message)

        self.course_service.update_course.assert_not_called()
        self.course_service.remove_course.assert_not_called()
        message.ack.assert_awaited_once()


class CourseEventFailureTest(IsolatedAsyncioTestCase):
    def setUp(self) -> None:
        self.course_service = Mock()
        self.consumer = _consumer(self.course_service)

    async def test_invalid_payload_goes_to_dlq(self) -> None:
        message = _message(PUBLISHED_ROUTING_KEY, body='{"courseId": 0}')

        with self.assertLogs("app.messaging.course_event_consumer", level="ERROR"):
            await self.consumer._on_message(message)

        message.reject.assert_awaited_once_with(requeue=False)
        message.ack.assert_not_awaited()
        self.course_service.update_course.assert_not_called()

    async def test_transient_failure_is_retried_before_succeeding(self) -> None:
        self.course_service.update_course.side_effect = [
            ConnectionError("일시 장애"),
            Mock(),
        ]
        message = _message(PUBLISHED_ROUTING_KEY)

        with self.assertLogs("app.messaging.course_event_consumer", level="WARNING"):
            await self.consumer._on_message(message)

        self.assertEqual(self.course_service.update_course.call_count, 2)
        message.ack.assert_awaited_once()
        message.reject.assert_not_awaited()

    async def test_exhausted_retries_go_to_dlq(self) -> None:
        self.course_service.update_course.side_effect = ConnectionError("계속 실패")
        message = _message(PUBLISHED_ROUTING_KEY)

        with self.assertLogs("app.messaging.course_event_consumer", level="ERROR"):
            await self.consumer._on_message(message)

        self.assertEqual(self.course_service.update_course.call_count, 3)
        message.reject.assert_awaited_once_with(requeue=False)
        message.ack.assert_not_awaited()


class CourseEventLifecycleTest(IsolatedAsyncioTestCase):
    async def test_start_requires_declare_first(self) -> None:
        consumer = _consumer(Mock())

        with self.assertRaises(RuntimeError):
            await consumer.start()
