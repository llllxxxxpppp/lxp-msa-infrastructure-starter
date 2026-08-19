"""환경 설정에 따른 강좌 데이터 공급자 생성."""

from pathlib import Path

from app.providers.course_provider import CourseProvider
from app.providers.http_course_provider import HttpCourseProvider
from app.providers.json_course_provider import JsonCourseProvider


def create_course_provider(
    provider_name: str,
    course_service_base_url: str,
    fixture_path: Path,
) -> CourseProvider:
    """설정값에 대응하는 강좌 데이터 공급자를 생성합니다."""

    if provider_name == "json":
        return JsonCourseProvider(fixture_path)
    if provider_name == "http":
        return HttpCourseProvider(course_service_base_url)
    raise ValueError("PROVIDER는 'http' 또는 'json'이어야 합니다.")
