"""강좌 조회와 벡터 검색 서비스."""

from threading import RLock

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings

from app.providers.course_provider import Course, CourseProvider


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
        self._courses = provider.get_courses()
        self._documents = [self._to_document(course) for course in self._courses]
        embeddings = OllamaEmbeddings(
            model=embedding_model,
            base_url=ollama_base_url,
        )
        self._vector_store = Chroma.from_documents(
            documents=self._documents,
            embedding=embeddings,
            ids=[str(course.course_id) for course in self._courses],
            collection_name="courses",
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

    def update_course(self, course_id: int) -> Course:
        """지정한 강좌를 다시 조회하여 벡터 저장소에 추가하거나 갱신합니다."""

        course = self._provider.get_course(course_id)
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
    ) -> dict[str, str | int]:
        with self._lock:
            course = next(
                course
                for course in self._courses
                if course.difficulty_label == difficulty_label
            )
            return course.model_dump(by_alias=True)
