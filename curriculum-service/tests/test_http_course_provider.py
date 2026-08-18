"""Course Service HTTP 공급자 테스트."""

from unittest import TestCase
from unittest.mock import Mock, patch

import httpx
from pydantic import ValidationError

from app.providers.http_course_provider import HttpCourseProvider


COURSE_DATA = {
    "courseId": 30017,
    "instructorId": 12,
    "title": "실무 SQL과 대시보드 만들기",
    "description": "SQL로 데이터를 추출하고 핵심 지표를 보여주는 대시보드를 설계합니다.",
    "category": "DATA_ANALYSIS",
    "categoryLabel": "데이터 분석",
    "difficulty": "PRACTICAL",
    "difficultyLabel": "실전",
    "durationMinutes": 420,
}


def _response(data: object, has_error: bool = False) -> Mock:
    response = Mock(spec=httpx.Response)
    response.json.return_value = data
    if has_error:
        response.raise_for_status.side_effect = httpx.HTTPStatusError(
            "Course Service 요청 실패",
            request=Mock(spec=httpx.Request),
            response=response,
        )
    return response


class HttpCourseProviderTest(TestCase):
    @patch("app.providers.http_course_provider.httpx.get")
    def test_get_courses_uses_for_rag_endpoint(self, mock_get: Mock) -> None:
        mock_get.return_value = _response([COURSE_DATA])
        provider = HttpCourseProvider("http://course-service/")

        courses = provider.get_courses()

        self.assertEqual(courses[0].course_id, 30017)
        mock_get.assert_called_once_with(
            "http://course-service/internal/courses/for-rag",
            timeout=10.0,
        )

    @patch("app.providers.http_course_provider.httpx.get")
    def test_get_course_uses_course_id_endpoint(self, mock_get: Mock) -> None:
        mock_get.return_value = _response(COURSE_DATA)
        provider = HttpCourseProvider("http://course-service")

        course = provider.get_course(30017)

        self.assertEqual(course.title, "실무 SQL과 대시보드 만들기")
        mock_get.assert_called_once_with(
            "http://course-service/internal/courses/30017/for-rag",
            timeout=10.0,
        )

    @patch("app.providers.http_course_provider.httpx.get")
    def test_get_courses_propagates_http_error(self, mock_get: Mock) -> None:
        mock_get.return_value = _response({}, has_error=True)

        with self.assertRaises(httpx.HTTPStatusError):
            HttpCourseProvider("http://course-service").get_courses()

    @patch("app.providers.http_course_provider.httpx.get")
    def test_get_courses_validates_response_contract(self, mock_get: Mock) -> None:
        invalid_course = {**COURSE_DATA, "durationMinutes": 0}
        mock_get.return_value = _response([invalid_course])

        with self.assertRaises(ValidationError):
            HttpCourseProvider("http://course-service").get_courses()

    def test_get_course_rejects_invalid_course_id(self) -> None:
        with self.assertRaises(ValueError):
            HttpCourseProvider("http://course-service").get_course(0)
