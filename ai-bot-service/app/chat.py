from fastapi import APIRouter
from langchain_ollama import ChatOllama
from pydantic import BaseModel

from app.documents import vector_store

router = APIRouter(
    prefix="/api/ai/courses/{course_id}/chat",
    tags=["chat"],
)

REFUSAL = "업로드된 강의 자료에서 해당 내용을 찾을 수 없습니다."

# 로컬 답변 모델
llm = ChatOllama(
    model="qwen3:8b",
    temperature=0,
    reasoning=False,
)


class ChatRequest(BaseModel):
    question: str


@router.post("")
def ask_question(course_id: int, request: ChatRequest) -> dict:
    """해당 강좌 자료를 검색하여 질문에 답변한다."""

    # course_id가 같은 자료 중 관련 청크 4개 검색
    results = vector_store.similarity_search_with_relevance_scores(
        query=request.question,
        k=4,
        filter={"course_id": course_id},
    )

    # 관련 자료가 없으면 답변 거절
    if not results:
        return {
            "answer": REFUSAL,
            "sources": [],
        }

    # 검색된 청크를 답변 모델에 전달할 문맥으로 구성
    context = "\n\n".join(
        document.page_content
        for document, _ in results
    )

    response = llm.invoke(
        f"""
다음 강의 자료만 사용해서 질문에 답변해.
자료에 답이 없으면 정확히 다음 문장만 출력해.
{REFUSAL}

강의 자료:
{context}

질문:
{request.question}
"""
    )

    answer = str(response.content).strip()

    if answer == REFUSAL:
        return {
            "answer": REFUSAL,
            "sources": [],
        }

    # 중복된 파일명과 페이지 제거
    source_values = {
        (
            document.metadata["filename"],
            document.metadata["page_number"],
        )
        for document, _ in results
    }

    sources = [
        {
            "filename": filename,
            "page_number": page_number,
        }
        for filename, page_number in source_values
    ]

    return {
        "answer": answer,
        "sources": sources,
    }