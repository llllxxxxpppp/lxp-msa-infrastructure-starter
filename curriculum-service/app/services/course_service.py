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
            ids=[course.id for course in self._courses],
            collection_name="courses",
        )

    @staticmethod
    def _to_document(course: Course) -> Document:
        content = (
            f"{course.title} {course.category} {course.difficulty} "
            f"{course.description} 소요 시간 {course.duration}"
        )
        return Document(page_content=content, metadata=course.model_dump())

    def search(
        self,
        query: str,
        difficulty: str,
        k: int = 2,
    ) -> list[Document]:
        return self._vector_store.similarity_search(
            query=query,
            k=k,
            filter={"difficulty": difficulty},
        )

    def get_documents_by_difficulty(self, difficulty: str) -> list[Document]:
        return [
            document
            for document in self._documents
            if document.metadata["difficulty"] == difficulty
        ]

    def get_first_course_by_difficulty(self, difficulty: str) -> dict[str, str]:
        course = next(
            course for course in self._courses if course.difficulty == difficulty
        )
        return course.model_dump()
