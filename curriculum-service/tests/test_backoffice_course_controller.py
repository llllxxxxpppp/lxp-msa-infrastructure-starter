"""백오피스 강좌 API 컨트롤러 테스트."""

from unittest import TestCase
from unittest.mock import Mock

from fastapi import HTTPException

from app.api.backoffice_course_controller import BackofficeCourseController
from app.providers.course_provider import Course


COURSE = Course.model_validate(
    {
        "courseId": 1,
        "instructorId": 12,
        "title": "테스트 강좌",
        "description": "테스트 설명",
        "category": "NEW_CATEGORY",
        "categoryLabel": "새 카테고리",
        "difficulty": "NEW_DIFFICULTY",
        "difficultyLabel": "새 난이도",
        "durationMinutes": 60,
    }
)


class BackofficeCourseControllerTest(TestCase):
    def setUp(self) -> None:
        self.course_service = Mock()
        self.controller = BackofficeCourseController(self.course_service)

    def test_routes_are_registered(self) -> None:
        methods_by_path = {
            (route.path, tuple(sorted(route.methods or [])))
            for route in self.controller.router.routes
        }

        self.assertIn(("/api/courses", ("GET",)), methods_by_path)
        self.assertIn(("/api/courses", ("PUT",)), methods_by_path)
        self.assertIn(("/api/courses/{courseId}", ("PUT",)), methods_by_path)

    def test_get_courses_returns_embedded_courses(self) -> None:
        self.course_service.get_embedded_courses.return_value = [COURSE]

        result = self.controller.get_courses()

        self.assertEqual(result, [COURSE])

    def test_update_all_courses_returns_updated_courses(self) -> None:
        self.course_service.update_all_courses.return_value = [COURSE]

        result = self.controller.update_all_courses()

        self.assertEqual(result, [COURSE])

    def test_update_course_returns_404_when_course_missing(self) -> None:
        self.course_service.update_course.return_value = None

        with self.assertRaises(HTTPException) as context:
            self.controller.update_course(404404)

        self.assertEqual(context.exception.status_code, 404)

    def test_update_course_returns_updated_course(self) -> None:
        self.course_service.update_course.return_value = COURSE

        result = self.controller.update_course(1)

        self.assertEqual(result, COURSE)
        self.course_service.update_course.assert_called_once_with(1)
