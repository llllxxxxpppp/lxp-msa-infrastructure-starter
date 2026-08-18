"""강좌 임베딩 갱신 서비스 테스트."""

from threading import RLock
from unittest import TestCase
from unittest.mock import Mock

from app.providers.course_provider import Course
from app.services.course_service import CourseService


def _course(course_id: int, title: str) -> Course:
    return Course.model_validate(
        {
            "courseId": course_id,
            "instructorId": 12,
            "title": title,
            "description": "테스트 설명",
            "category": "CATEGORY",
            "categoryLabel": "카테고리",
            "difficulty": "DIFFICULTY",
            "difficultyLabel": "난이도",
            "durationMinutes": 60,
        }
    )


def _service(initial_courses: list[Course]) -> tuple[CourseService, Mock, Mock]:
    provider = Mock()
    vector_store = Mock()
    service = CourseService.__new__(CourseService)
    service._provider = provider
    service._lock = RLock()
    service._courses = list(initial_courses)
    service._documents = [service._to_document(course) for course in initial_courses]
    service._vector_store = vector_store
    return service, provider, vector_store


class CourseServiceUpdatesTest(TestCase):
    def test_update_course_keeps_index_when_course_missing(self) -> None:
        existing_courses = [_course(1, "기존 강좌")]
        service, provider, vector_store = _service(existing_courses)
        provider.get_course.return_value = None

        result = service.update_course(404404)

        self.assertIsNone(result)
        self.assertEqual(service.get_embedded_courses(), existing_courses)
        vector_store.add_documents.assert_not_called()
        vector_store.update_documents.assert_not_called()

    def test_remove_course_deletes_document(self) -> None:
        service, _, vector_store = _service([_course(1, "기존 강좌"), _course(2, "남을 강좌")])

        removed = service.remove_course(1)

        self.assertTrue(removed)
        self.assertEqual(service.get_embedded_courses(), [_course(2, "남을 강좌")])
        vector_store.delete.assert_called_once_with(ids=["1"])
        remaining_documents = service.get_documents_by_difficulty_label("난이도")
        self.assertEqual(
            [document.metadata["courseId"] for document in remaining_documents],
            [2],
        )

    def test_remove_course_ignores_unknown_course(self) -> None:
        existing_courses = [_course(1, "기존 강좌")]
        service, _, vector_store = _service(existing_courses)

        removed = service.remove_course(404404)

        self.assertFalse(removed)
        self.assertEqual(service.get_embedded_courses(), existing_courses)
        vector_store.delete.assert_not_called()

    def test_remove_course_is_idempotent(self) -> None:
        service, _, vector_store = _service([_course(1, "기존 강좌")])

        self.assertTrue(service.remove_course(1))
        self.assertFalse(service.remove_course(1))

        vector_store.delete.assert_called_once_with(ids=["1"])
        self.assertEqual(service.get_embedded_courses(), [])

    def test_update_all_courses_replaces_documents(self) -> None:
        service, provider, vector_store = _service([_course(1, "기존 강좌")])
        updated_courses = [_course(2, "새 강좌")]
        provider.get_courses.return_value = updated_courses

        result = service.update_all_courses()

        self.assertEqual(result, updated_courses)
        self.assertEqual(service.get_embedded_courses(), updated_courses)
        vector_store.reset_collection.assert_called_once_with()
        added_document = vector_store.add_documents.call_args.kwargs["documents"][0]
        self.assertEqual(added_document.metadata["courseId"], 2)
        self.assertEqual(vector_store.add_documents.call_args.kwargs["ids"], ["2"])

    def test_update_existing_course_updates_document(self) -> None:
        service, provider, vector_store = _service([_course(1, "기존 강좌")])
        updated_course = _course(1, "수정된 강좌")
        provider.get_course.return_value = updated_course

        result = service.update_course(1)

        self.assertEqual(result, updated_course)
        self.assertEqual(service.get_embedded_courses(), [updated_course])
        vector_store.update_documents.assert_called_once()
        self.assertEqual(vector_store.update_documents.call_args.kwargs["ids"], ["1"])
        vector_store.add_documents.assert_not_called()

    def test_update_new_course_adds_document(self) -> None:
        service, provider, vector_store = _service([_course(1, "기존 강좌")])
        new_course = _course(2, "추가 강좌")
        provider.get_course.return_value = new_course

        result = service.update_course(2)

        self.assertEqual(result, new_course)
        self.assertEqual(service.get_embedded_courses(), [_course(1, "기존 강좌"), new_course])
        vector_store.add_documents.assert_called_once()
        self.assertEqual(vector_store.add_documents.call_args.kwargs["ids"], ["2"])
        vector_store.update_documents.assert_not_called()
