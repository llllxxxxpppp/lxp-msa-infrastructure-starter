#!/usr/bin/env python3

"""LangGraph를 이용한 맞춤형 커리큘럼 설계 API."""

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
)
from app.providers.course_provider_factory import create_course_provider
from app.services.course_service import CourseService
from app.services.llm_service import LlmService
from app.workflows.curriculum_workflow import CurriculumWorkflow

COURSES_FIXTURE_PATH = (
    Path(__file__).resolve().parent.parent / "tests" / "fixtures" / "courses.json"
)


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
    course_service.load_all_courses()
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

    application = FastAPI(title="맞춤형 커리큘럼 설계 봇", version="0.1.0")
    application.include_router(controller.router)
    application.include_router(backoffice_controller.router)

    return application


app = create_app()
