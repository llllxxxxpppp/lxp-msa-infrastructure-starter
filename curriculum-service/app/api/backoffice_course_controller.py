"""임베딩 강좌 데이터를 관리하는 백오피스 API."""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Path

from app.api.auth_dependencies import require_admin
from app.providers.course_provider import Course
from app.services.course_service import CourseService


class BackofficeCourseController:
    """임베딩된 강좌의 조회와 갱신 API를 제공합니다."""

    def __init__(self, course_service: CourseService) -> None:
        self._course_service = course_service
        self.router = APIRouter(
            prefix="/api",
            tags=["backoffice"],
            dependencies=[Depends(require_admin)],
        )
        self.router.add_api_route(
            "/courses",
            self.get_courses,
            methods=["GET"],
            response_model=list[Course],
        )
        self.router.add_api_route(
            "/courses",
            self.update_all_courses,
            methods=["PUT"],
            response_model=list[Course],
        )
        self.router.add_api_route(
            "/courses/{courseId}",
            self.update_course,
            methods=["PUT"],
            response_model=Course,
        )

    def get_courses(self) -> list[Course]:
        return self._course_service.get_embedded_courses()

    def update_all_courses(self) -> list[Course]:
        return self._course_service.update_all_courses()

    def update_course(
        self,
        course_id: Annotated[int, Path(alias="courseId", ge=1)],
    ) -> Course:
        course = self._course_service.update_course(course_id)
        if course is None:
            raise HTTPException(status_code=404, detail="강좌를 찾을 수 없습니다.")
        return course
