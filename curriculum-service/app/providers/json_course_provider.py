"""JSON 파일 기반 강좌 데이터 공급자."""

import json
from pathlib import Path

from app.providers.course_provider import Course, CourseProvider


class JsonCourseProvider(CourseProvider):
    """JSON fixture에서 강좌 데이터를 읽습니다."""

    def __init__(self, fixture_path: Path) -> None:
        self.fixture_path = fixture_path

    def get_courses(self) -> list[Course]:
        with self.fixture_path.open(encoding="utf-8") as fixture_file:
            course_data = json.load(fixture_file)

        return [Course.model_validate(item) for item in course_data]
