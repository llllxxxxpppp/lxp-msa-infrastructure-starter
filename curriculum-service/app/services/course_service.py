"""강좌 조회와 벡터 검색 서비스."""

import logging
import time
from threading import RLock

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings

from app.providers.course_provider import Course, CourseProvider

logger = logging.getLogger(__name__)


class CourseService:
    """강좌 데이터와 검색용 벡터 저장소를 관리합니다."""

    def __init__(
        self,
        provider: CourseProvider,
        ollama_base_url: str,
        embedding_model: str,
    ) -> None:
        self._provider = provider
        self._lock = RLock()
        self._courses: list[Course] = []
        self._documents: list[Document] = []
        self._vector_store = Chroma(
            collection_name="courses",
            embedding_function=OllamaEmbeddings(
                model=embedding_model,
                base_url=ollama_base_url,
            ),
        )

    @staticmethod
    def _to_document(course: Course) -> Document:
        content = (
            f"강좌 ID {course.course_id} 강사 ID {course.instructor_id} "
            f"제목 {course.title} 설명 {course.description} "
            f"카테고리 {course.category} 카테고리명 {course.category_label} "
            f"난이도 {course.difficulty} 난이도명 {course.difficulty_label} "
            f"소요 시간 {course.duration_minutes}분"
        )
        return Document(
            page_content=content,
            metadata=course.model_dump(by_alias=True),
        )

    def search(
        self,
        query: str,
        difficulty_label: str,
        k: int = 2,
    ) -> list[Document]:
        with self._lock:
            return self._vector_store.similarity_search(
                query=query,
                k=k,
                filter={"difficultyLabel": difficulty_label},
            )

    def get_embedded_courses(self) -> list[Course]:
        """현재 벡터 저장소에 반영된 강좌를 반환합니다."""

        with self._lock:
            return list(self._courses)

    def update_all_courses(self) -> list[Course]:
        """전체 강좌를 다시 조회하여 벡터 저장소를 재구성합니다."""

        courses = self._provider.get_courses()
        documents = [self._to_document(course) for course in courses]
        with self._lock:
            self._vector_store.reset_collection()
            if documents:
                self._vector_store.add_documents(
                    documents=documents,
                    ids=[str(course.course_id) for course in courses],
                )
            self._courses = courses
            self._documents = documents
            return list(self._courses)

    def load_all_courses(
        self,
        attempts: int = 3,
        delay_seconds: float = 2.0,
    ) -> list[Course]:
        """전체 강좌를 적재합니다. 끝내 실패하면 빈 인덱스를 유지합니다."""

        for attempt in range(1, attempts + 1):
            try:
                return self.update_all_courses()
            except Exception:
                if attempt == attempts:
                    logger.exception(
                        "초기 적재에 %d회 실패해 빈 인덱스로 기동합니다.", attempts
                    )
                    return self.get_embedded_courses()
                logger.warning(
                    "초기 적재 %d/%d회 실패. %.1f초 후 재시도합니다.",
                    attempt,
                    attempts,
                    delay_seconds,
                )
                time.sleep(delay_seconds)
        return self.get_embedded_courses()

    def update_course(self, course_id: int) -> Course | None:
        """지정한 강좌를 갱신합니다. 없으면 인덱스를 건드리지 않고 None을 반환합니다."""

        course = self._provider.get_course(course_id)
        if course is None:
            return None
        document = self._to_document(course)
        with self._lock:
            existing_index = next(
                (
                    index
                    for index, existing in enumerate(self._courses)
                    if existing.course_id == course.course_id
                ),
                None,
            )
            if existing_index is None:
                self._vector_store.add_documents(
                    documents=[document],
                    ids=[str(course.course_id)],
                )
                self._courses.append(course)
                self._documents.append(document)
            else:
                self._vector_store.update_documents(
                    ids=[str(course.course_id)],
                    documents=[document],
                )
                self._courses[existing_index] = course
                self._documents[existing_index] = document
        return course

    def remove_course(self, course_id: int) -> bool:
        """지정한 강좌를 제거합니다. 인덱스에 없던 강좌면 False를 반환합니다."""

        with self._lock:
            existing_index = next(
                (
                    index
                    for index, existing in enumerate(self._courses)
                    if existing.course_id == course_id
                ),
                None,
            )
            if existing_index is None:
                return False
            self._vector_store.delete(ids=[str(course_id)])
            del self._courses[existing_index]
            del self._documents[existing_index]
            return True

    def get_documents_by_difficulty_label(
        self,
        difficulty_label: str,
    ) -> list[Document]:
        with self._lock:
            return [
                document
                for document in self._documents
                if document.metadata["difficultyLabel"] == difficulty_label
            ]

    def get_first_course_by_difficulty_label(
        self,
        difficulty_label: str,
    ) -> dict[str, str | int] | None:
        """해당 난이도의 첫 강좌를 반환합니다. 없으면 None을 반환합니다."""

        with self._lock:
            course = next(
                (
                    course
                    for course in self._courses
                    if course.difficulty_label == difficulty_label
                ),
                None,
            )
            return course.model_dump(by_alias=True) if course else None
