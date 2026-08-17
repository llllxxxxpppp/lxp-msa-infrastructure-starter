"""강좌 조회와 벡터 검색 서비스."""

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
        return self._vector_store.similarity_search(
            query=query,
            k=k,
            filter={"difficultyLabel": difficulty_label},
        )

    def get_documents_by_difficulty_label(
        self,
        difficulty_label: str,
    ) -> list[Document]:
        return [
            document
            for document in self._documents
            if document.metadata["difficultyLabel"] == difficulty_label
        ]

    def get_first_course_by_difficulty_label(
        self,
        difficulty_label: str,
    ) -> dict[str, str | int]:
        course = next(
            course
            for course in self._courses
            if course.difficulty_label == difficulty_label
        )
        return course.model_dump(by_alias=True)
