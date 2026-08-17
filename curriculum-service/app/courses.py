"""강좌 데이터와 인메모리 벡터 저장소."""

import json
from pathlib import Path

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings

from app.config import OLLAMA_BASE_URL, OLLAMA_EMBEDDING_MODEL

COURSES_FIXTURE_PATH = (
    Path(__file__).resolve().parent.parent / "tests" / "fixtures" / "courses.json"
)

with COURSES_FIXTURE_PATH.open(encoding="utf-8") as fixture_file:
    COURSES: list[dict[str, str]] = json.load(fixture_file)


def _course_document(course: dict[str, str]) -> Document:
    content = (
        f"{course['title']} {course['category']} {course['difficulty']} "
        f"{course['description']} 소요 시간 {course['duration']}"
    )
    return Document(page_content=content, metadata=course)


COURSE_DOCUMENTS = [_course_document(course) for course in COURSES]
embeddings = OllamaEmbeddings(
    model=OLLAMA_EMBEDDING_MODEL,
    base_url=OLLAMA_BASE_URL,
)
vector_store = Chroma.from_documents(
    documents=COURSE_DOCUMENTS,
    embedding=embeddings,
    ids=[course["id"] for course in COURSES],
    collection_name="courses",
)


def search_courses(query: str, difficulty: str, k: int = 2) -> list[Document]:
    return vector_store.similarity_search(
        query=query,
        k=k,
        filter={"difficulty": difficulty},
    )
