#!/usr/bin/env python3

"""LangGraph를 이용한 맞춤형 커리큘럼 설계 API."""

import asyncio
from collections.abc import AsyncIterator, Callable
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from langgraph.checkpoint.memory import InMemorySaver

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
    SESSION_CLEANUP_INTERVAL_SECONDS,
    SESSION_TIMEOUT_SECONDS,
)
from app.messaging.course_event_consumer import CourseEventConsumer
from app.providers.course_provider_factory import create_course_provider
from app.services.course_service import CourseService
from app.services.conversation_session_service import ConversationSessionService
from app.services.llm_service import LlmService
from app.workflows.curriculum_workflow import CurriculumWorkflow

COURSES_FIXTURE_PATH = (
    Path(__file__).resolve().parent.parent / "tests" / "fixtures" / "courses.json"
)


def build_lifespan(
    course_service: CourseService,
    consumer: CourseEventConsumer,
    session_service: ConversationSessionService,
) -> Callable[[FastAPI], AsyncIterator[None]]:
    """기동·종료 순서를 정의합니다."""

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        cleanup_task: asyncio.Task[None] | None = None
        try:
            await consumer.declare()
            await asyncio.to_thread(course_service.load_all_courses)
            await consumer.start()
            cleanup_task = asyncio.create_task(session_service.run_cleanup())
            yield
        finally:
            try:
                if cleanup_task is not None:
                    cleanup_task.cancel()
                    try:
                        await cleanup_task
                    except asyncio.CancelledError:
                        pass
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
    checkpointer = InMemorySaver()
    graph = workflow.build(checkpointer)
    session_service = ConversationSessionService(
        graph=graph,
        checkpointer=checkpointer,
        timeout_seconds=SESSION_TIMEOUT_SECONDS,
        cleanup_interval_seconds=SESSION_CLEANUP_INTERVAL_SECONDS,
    )
    controller = ChatController(
        session_service=session_service,
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
        lifespan=build_lifespan(course_service, consumer, session_service),
    )
    application.include_router(controller.router)
    application.include_router(backoffice_controller.router)

    return application


app = create_app()
