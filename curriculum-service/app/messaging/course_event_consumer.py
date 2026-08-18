"""course-service의 강좌 변경 이벤트 소비자."""

import asyncio
import logging
import time
from datetime import datetime
from uuid import UUID

import aio_pika
from aio_pika import ExchangeType
from aio_pika.abc import AbstractIncomingMessage
from pydantic import BaseModel, ConfigDict, Field, ValidationError

from app.services.course_service import CourseService

logger = logging.getLogger(__name__)

# course-service(발행)와 합의해야 하는 계약 값
EXCHANGE = "course.events"
PUBLISHED_ROUTING_KEY = "course.published"
UNPUBLISHED_ROUTING_KEY = "course.unpublished"
DELETED_ROUTING_KEY = "course.deleted"

# 소비자인 봇이 선언하는 값
QUEUE = "curriculum.course-sync"
DLX = "curriculum.course-sync.dlx"
DLQ = "curriculum.course-sync.dlq"

ROUTING_KEYS = (
    PUBLISHED_ROUTING_KEY,
    UNPUBLISHED_ROUTING_KEY,
    DELETED_ROUTING_KEY,
)


class CourseEvent(BaseModel):
    """강좌 변경 이벤트 페이로드."""

    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    event_id: UUID = Field(alias="eventId")
    occurred_at: datetime = Field(alias="occurredAt")
    course_id: int = Field(alias="courseId", ge=1)


class CourseEventConsumer:
    """강좌 변경 이벤트를 받아 벡터 인덱스에 반영합니다."""

    def __init__(
        self,
        course_service: CourseService,
        host: str,
        port: int,
        username: str,
        password: str,
        prefetch_count: int = 10,
        attempts: int = 3,
        delay_seconds: float = 1.0,
    ) -> None:
        self._course_service = course_service
        self._host = host
        self._port = port
        self._username = username
        self._password = password
        self._prefetch_count = prefetch_count
        self._attempts = attempts
        self._delay_seconds = delay_seconds
        self._connection: aio_pika.abc.AbstractRobustConnection | None = None
        self._queue: aio_pika.abc.AbstractQueue | None = None

    async def declare(self) -> None:
        """익스체인지·큐·바인딩·DLQ를 선언합니다.

        이 시점부터 발행된 이벤트가 큐에 쌓이므로 초기 적재보다 먼저 호출해야 합니다.
        """

        self._connection = await aio_pika.connect_robust(
            host=self._host,
            port=self._port,
            login=self._username,
            password=self._password,
        )
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=self._prefetch_count)

        dead_letter_exchange = await channel.declare_exchange(
            DLX,
            ExchangeType.DIRECT,
            durable=True,
        )
        dead_letter_queue = await channel.declare_queue(DLQ, durable=True)
        await dead_letter_queue.bind(dead_letter_exchange, routing_key=QUEUE)

        # 발행 측 TopicExchange(name, durable=true, autoDelete=false)와 인자가 같아야 한다.
        course_exchange = await channel.declare_exchange(
            EXCHANGE,
            ExchangeType.TOPIC,
            durable=True,
        )
        self._queue = await channel.declare_queue(
            QUEUE,
            durable=True,
            arguments={
                "x-dead-letter-exchange": DLX,
                "x-dead-letter-routing-key": QUEUE,
            },
        )
        for routing_key in ROUTING_KEYS:
            await self._queue.bind(course_exchange, routing_key=routing_key)

        logger.info("강좌 이벤트 큐를 선언했습니다. queue=%s", QUEUE)

    async def start(self) -> None:
        """소비를 시작합니다. declare()와 초기 적재를 마친 뒤 호출해야 합니다."""

        if self._queue is None:
            raise RuntimeError("declare()를 먼저 호출해야 합니다.")
        await self._queue.consume(self._on_message)
        logger.info("강좌 이벤트 소비를 시작했습니다.")

    async def stop(self) -> None:
        """브로커 연결을 정리합니다."""

        if self._connection is not None:
            await self._connection.close()
            self._connection = None
            self._queue = None

    async def _on_message(self, message: AbstractIncomingMessage) -> None:
        try:
            event = CourseEvent.model_validate_json(message.body)
        except ValidationError:
            logger.exception(
                "이벤트 형식이 올바르지 않아 DLQ로 보냅니다. body=%r",
                message.body,
            )
            await message.reject(requeue=False)
            return

        try:
            # CourseService는 동기이고 내부에서 HTTP·임베딩을 호출한다.
            # 이벤트 루프에서 직접 돌리면 그동안 /chat이 멈춘다.
            await asyncio.to_thread(self._apply_with_retry, message.routing_key, event)
        except Exception:
            logger.exception(
                "이벤트 처리에 실패해 DLQ로 보냅니다. "
                "routingKey=%s, courseId=%d, eventId=%s",
                message.routing_key,
                event.course_id,
                event.event_id,
            )
            await message.reject(requeue=False)
            return

        await message.ack()

    def _apply_with_retry(self, routing_key: str, event: CourseEvent) -> None:
        """일시 장애를 흡수하기 위해 짧게 재시도합니다.

        간격은 course-service의 listener.simple.retry 설정과 같게 맞췄습니다.
        requeue 대신 재시도하는 것은 실패한 메시지가 브로커와 봇 사이를
        무한히 오가는 것을 막기 위해서입니다.
        """

        for attempt in range(1, self._attempts + 1):
            try:
                self._apply(routing_key, event)
                return
            except Exception:
                if attempt == self._attempts:
                    raise
                delay = self._delay_seconds * (2 ** (attempt - 1))
                logger.warning(
                    "이벤트 처리 %d/%d회 실패. %.1f초 후 재시도합니다. courseId=%d",
                    attempt,
                    self._attempts,
                    delay,
                    event.course_id,
                )
                time.sleep(delay)

    def _apply(self, routing_key: str, event: CourseEvent) -> None:
        if routing_key == PUBLISHED_ROUTING_KEY:
            if self._course_service.update_course(event.course_id) is None:
                # 조회 시점에 이미 내려간 강좌다. 계약상 건너뛰기가 맞다.
                logger.info(
                    "공개된 강좌를 찾지 못해 건너뜁니다. courseId=%d",
                    event.course_id,
                )
            return

        if routing_key in (UNPUBLISHED_ROUTING_KEY, DELETED_ROUTING_KEY):
            if not self._course_service.remove_course(event.course_id):
                logger.info(
                    "인덱스에 없는 강좌라 제거를 건너뜁니다. courseId=%d",
                    event.course_id,
                )
            return

        logger.debug("처리 대상이 아닌 라우팅 키입니다. routingKey=%s", routing_key)
