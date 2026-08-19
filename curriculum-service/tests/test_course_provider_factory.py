"""강좌 데이터 공급자 선택 테스트."""

from pathlib import Path
from unittest import TestCase

from app.providers.course_provider_factory import create_course_provider
from app.providers.http_course_provider import HttpCourseProvider
from app.providers.json_course_provider import JsonCourseProvider


class CourseProviderFactoryTest(TestCase):
    def test_http_is_selected_for_default_name(self) -> None:
        provider = create_course_provider(
            provider_name="http",
            course_service_base_url="http://course-service",
            fixture_path=Path("courses.json"),
        )

        self.assertIsInstance(provider, HttpCourseProvider)

    def test_json_is_selected_for_json_name(self) -> None:
        provider = create_course_provider(
            provider_name="json",
            course_service_base_url="http://course-service",
            fixture_path=Path("courses.json"),
        )

        self.assertIsInstance(provider, JsonCourseProvider)

    def test_unknown_provider_is_rejected(self) -> None:
        with self.assertRaises(ValueError):
            create_course_provider(
                provider_name="fixture",
                course_service_base_url="http://course-service",
                fixture_path=Path("courses.json"),
            )
