"""강좌 데이터와 인메모리 벡터 저장소."""

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings

from app.config import OLLAMA_BASE_URL, OLLAMA_EMBEDDING_MODEL

COURSES = [
    {
        "id": "data-101",
        "title": "엑셀로 시작하는 데이터 분석",
        "category": "데이터 분석",
        "difficulty": "입문",
        "duration": "4시간",
        "description": "함수, 피벗 테이블, 차트를 활용해 업무 데이터를 정리합니다.",
    },
    {
        "id": "data-201",
        "title": "실무 SQL과 대시보드",
        "category": "데이터 분석",
        "difficulty": "실전",
        "duration": "8시간",
        "description": "SQL로 데이터를 추출하고 핵심 지표 대시보드를 만듭니다.",
    },
    {
        "id": "data-301",
        "title": "Python 기반 예측 분석",
        "category": "데이터 분석",
        "difficulty": "심화",
        "duration": "12시간",
        "description": "Python과 머신러닝으로 예측 모델을 만들고 평가합니다.",
    },
    {
        "id": "marketing-101",
        "title": "디지털 마케팅 입문",
        "category": "마케팅",
        "difficulty": "입문",
        "duration": "3시간",
        "description": "퍼널, 채널, 핵심 마케팅 지표의 기본 개념을 학습합니다.",
    },
    {
        "id": "marketing-201",
        "title": "GA4로 하는 캠페인 성과 분석",
        "category": "마케팅",
        "difficulty": "실전",
        "duration": "6시간",
        "description": "GA4로 캠페인을 분석하고 개선안을 도출합니다.",
    },
    {
        "id": "marketing-301",
        "title": "마케팅 자동화와 실험 설계",
        "category": "마케팅",
        "difficulty": "심화",
        "duration": "9시간",
        "description": "고객 세분화, 자동화 시나리오, A/B 테스트를 설계합니다.",
    },
    {
        "id": "backend-101",
        "title": "웹과 REST API 기초",
        "category": "백엔드 개발",
        "difficulty": "입문",
        "duration": "5시간",
        "description": "HTTP, REST, 데이터베이스 등 백엔드의 기초를 학습합니다.",
    },
    {
        "id": "backend-201",
        "title": "FastAPI 실전 프로젝트",
        "category": "백엔드 개발",
        "difficulty": "실전",
        "duration": "10시간",
        "description": "FastAPI로 인증과 데이터베이스를 포함한 API를 구현합니다.",
    },
    {
        "id": "backend-301",
        "title": "확장 가능한 백엔드 아키텍처",
        "category": "백엔드 개발",
        "difficulty": "심화",
        "duration": "12시간",
        "description": "캐시, 메시지 큐, 관측성을 적용해 서비스를 확장합니다.",
    },
    {
        "id": "product-101",
        "title": "프로덕트 매니지먼트 기초",
        "category": "프로덕트",
        "difficulty": "입문",
        "duration": "4시간",
        "description": "고객 문제 정의부터 제품 지표까지 기본기를 익힙니다.",
    },
    {
        "id": "product-201",
        "title": "데이터 기반 제품 개선",
        "category": "프로덕트",
        "difficulty": "실전",
        "duration": "7시간",
        "description": "제품 데이터를 분석하고 가설과 실험을 설계합니다.",
    },
    {
        "id": "product-301",
        "title": "제품 전략과 로드맵",
        "category": "프로덕트",
        "difficulty": "심화",
        "duration": "8시간",
        "description": "사업 목표와 고객 가치를 연결한 제품 전략을 수립합니다.",
    },
]


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
