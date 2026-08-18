"""Course Service HTTP API 기반 강좌 데이터 공급자."""

import httpx
from pydantic import TypeAdapter

from app.providers.course_provider import Course, CourseProvider


class HttpCourseProvider(CourseProvider):
    """Course Service의 RAG용 내부 API에서 강좌 데이터를 조회합니다."""

    def __init__(self, base_url: str, timeout: float = 10.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    def get_courses(self) -> list[Course]:
        response = httpx.get(
            f"{self._base_url}/internal/courses/for-rag",
            timeout=self._timeout,
        )
        response.raise_for_status()
        return TypeAdapter(list[Course]).validate_python(response.json())

    def get_course(self, course_id: int) -> Course:
        if course_id < 1:
            raise ValueError("course_id는 1 이상이어야 합니다.")
        response = httpx.get(
            f"{self._base_url}/internal/courses/{course_id}/for-rag",
            timeout=self._timeout,
        )
        response.raise_for_status()
        return Course.model_validate(response.json())
