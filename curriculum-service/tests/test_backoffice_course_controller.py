"""백오피스 강좌 API 컨트롤러 테스트."""

from unittest import TestCase
from unittest.mock import Mock

from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient

from app.api.backoffice_course_controller import BackofficeCourseController
from app.api.chat_controller import ChatController
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
        app = FastAPI()
        app.include_router(self.controller.router)
        self.client = TestClient(app)

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

    def test_all_routes_allow_authenticated_admin(self) -> None:
        self.course_service.get_embedded_courses.return_value = [COURSE]
        self.course_service.update_all_courses.return_value = [COURSE]
        self.course_service.update_course.return_value = COURSE
        headers = {"X-User-Id": " 1 ", "X-Role": "ROLE_ADMIN"}

        responses = [
            self.client.get("/api/courses", headers=headers),
            self.client.put("/api/courses", headers=headers),
            self.client.put("/api/courses/1", headers=headers),
        ]

        self.assertEqual([response.status_code for response in responses], [200, 200, 200])
        self.course_service.get_embedded_courses.assert_called_once_with()
        self.course_service.update_all_courses.assert_called_once_with()
        self.course_service.update_course.assert_called_once_with(1)

    def test_invalid_user_id_returns_401_without_calling_service(self) -> None:
        invalid_user_ids = [None, "", " ", "0", "-1", "not-a-number"]

        for user_id in invalid_user_ids:
            with self.subTest(user_id=user_id):
                self.course_service.reset_mock()
                headers = {"X-Role": "ROLE_ADMIN"}
                if user_id is not None:
                    headers["X-User-Id"] = user_id

                response = self.client.get("/api/courses", headers=headers)

                self.assertEqual(response.status_code, 401)
                self.course_service.get_embedded_courses.assert_not_called()

    def test_missing_or_invalid_role_returns_403_without_calling_service(self) -> None:
        invalid_roles = [None, "", "role_admin", "ROLE_admin", " ROLE_ADMIN "]

        for role in invalid_roles:
            with self.subTest(role=role):
                self.course_service.reset_mock()
                headers = {"X-User-Id": "1"}
                if role is not None:
                    headers["X-Role"] = role

                response = self.client.put("/api/courses", headers=headers)

                self.assertEqual(response.status_code, 403)
                self.course_service.update_all_courses.assert_not_called()

    def test_health_does_not_require_authentication(self) -> None:
        app = FastAPI()
        chat_controller = ChatController(
            graph=Mock(),
            ollama_model="test-model",
            ollama_base_url="http://ollama.test",
        )
        app.include_router(chat_controller.router)

        response = TestClient(app).get("/health")

        self.assertEqual(response.status_code, 200)

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
