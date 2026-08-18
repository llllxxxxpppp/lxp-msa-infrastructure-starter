#!/usr/bin/env python3

"""LangGraph를 이용한 맞춤형 커리큘럼 설계 API."""

import asyncio
from collections.abc import AsyncIterator, Callable
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI

from app.api.backoffice_course_controller import BackofficeCourseController
from app.api.chat_controller import ChatController
from app.config import (
    COURSE_SERVICE_BASE_URL,
    OLLAMA_BASE_URL,
    OLLAMA_EMBEDDING_MODEL,
    OLLAMA_MODEL,
    PROVIDER,
    RABBITMQ_HOST,
    RABBITMQ_PASSWORD,
    RABBITMQ_PORT,
    RABBITMQ_USERNAME,
)
from app.messaging.course_event_consumer import CourseEventConsumer
from app.providers.course_provider_factory import create_course_provider
from app.services.course_service import CourseService
from app.services.llm_service import LlmService
from app.workflows.curriculum_workflow import CurriculumWorkflow

COURSES_FIXTURE_PATH = (
    Path(__file__).resolve().parent.parent / "tests" / "fixtures" / "courses.json"
)


def build_lifespan(
    course_service: CourseService,
    consumer: CourseEventConsumer,
) -> Callable[[FastAPI], AsyncIterator[None]]:
    """기동·종료 순서를 정의합니다.

    순서가 중요합니다. 큐를 먼저 선언해야 적재하는 동안 발생한 이벤트가 큐에
    쌓이고, 소비를 마지막에 시작해야 적재 결과가 이벤트를 덮어쓰지 않습니다.
    """

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        try:
            await consumer.declare()
            # 동기 함수이고 재시도에 sleep을 쓴다. 이벤트 루프를 막지 않게 넘긴다.
            await asyncio.to_thread(course_service.load_all_courses)
            await consumer.start()
            yield
        finally:
            await consumer.stop()

    return lifespan


def create_app() -> FastAPI:
    """애플리케이션 의존성을 구성하고 FastAPI 인스턴스를 생성합니다."""

    course_provider = create_course_provider(
        provider_name=PROVIDER,
        course_service_base_url=COURSE_SERVICE_BASE_URL,
        fixture_path=COURSES_FIXTURE_PATH,
    )
    course_service = CourseService(
        provider=course_provider,
        ollama_base_url=OLLAMA_BASE_URL,
        embedding_model=OLLAMA_EMBEDDING_MODEL,
    )
    llm_service = LlmService(
        model=OLLAMA_MODEL,
        base_url=OLLAMA_BASE_URL,
    )
    workflow = CurriculumWorkflow(
        course_service=course_service,
        llm_service=llm_service,
    )
    controller = ChatController(
        graph=workflow.build(),
        ollama_model=OLLAMA_MODEL,
        ollama_base_url=OLLAMA_BASE_URL,
    )
    backoffice_controller = BackofficeCourseController(course_service)
    consumer = CourseEventConsumer(
        course_service=course_service,
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        username=RABBITMQ_USERNAME,
        password=RABBITMQ_PASSWORD,
    )

    application = FastAPI(
        title="맞춤형 커리큘럼 설계 봇",
        version="0.1.0",
        lifespan=build_lifespan(course_service, consumer),
    )
    application.include_router(controller.router)
    application.include_router(backoffice_controller.router)

    return application


app = create_app()
