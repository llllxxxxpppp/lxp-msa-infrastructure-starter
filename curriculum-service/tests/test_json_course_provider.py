"""JSON fixture 강좌 공급자 테스트."""

import json
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase

from app.providers.json_course_provider import JsonCourseProvider


COURSE_DATA = {
    "courseId": 1,
    "instructorId": 12,
    "title": "엑셀로 시작하는 데이터 분석",
    "description": "함수, 피벗 테이블, 차트를 활용해 업무 데이터를 정리합니다.",
    "category": "DATA_ANALYSIS",
    "categoryLabel": "데이터 분석",
    "difficulty": "BEGINNER",
    "difficultyLabel": "입문",
    "durationMinutes": 240,
}


class JsonCourseProviderTest(TestCase):
    def setUp(self) -> None:
        self._directory = TemporaryDirectory()
        self.addCleanup(self._directory.cleanup)
        fixture_path = Path(self._directory.name) / "courses.json"
        fixture_path.write_text(
            json.dumps([COURSE_DATA], ensure_ascii=False),
            encoding="utf-8",
        )
        self.provider = JsonCourseProvider(fixture_path)

    def test_get_course_returns_matching_course(self) -> None:
        course = self.provider.get_course(1)

        self.assertIsNotNone(course)
        self.assertEqual(course.title, "엑셀로 시작하는 데이터 분석")

    def test_get_course_returns_none_when_missing(self) -> None:
        self.assertIsNone(self.provider.get_course(404404))
