import os
import json
import logging  # [추가]

from fastapi import APIRouter
from langchain_ollama import ChatOllama
from pydantic import BaseModel
from fastapi.responses import StreamingResponse

from app.documents import vector_store
from prometheus_client import Counter

router = APIRouter(
    prefix="/api/ai/courses/{course_id}/chat",
    tags=["chat"],
)

logger = logging.getLogger("uvicorn.error")  # [변경]

# [추가] LLM 토큰 사용량 Prometheus Metric
LLM_INPUT_TOKENS = Counter(
    "ai_llm_input_tokens_total",
    "LLM input token count",
)

LLM_OUTPUT_TOKENS = Counter(
    "ai_llm_output_tokens_total",
    "LLM output token count",
)

REFUSAL = "업로드된 강의 자료에서 해당 내용을 찾을 수 없습니다."

# [추가] 이 점수보다 관련도가 낮은 검색 결과는 답변 근거에서 제외한다.
MIN_RELEVANCE_SCORE = float(
    os.getenv("RAG_MIN_RELEVANCE_SCORE", "0.11")
)

# Ollama 답변 모델
llm = ChatOllama(
    # [수정] compose.yaml의 OLLAMA_CHAT_MODEL 사용
    model=os.getenv("OLLAMA_CHAT_MODEL", "qwen3.5:4b"),

    # Docker 실행 시 compose.yaml의 http://ollama:11434 사용
    base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434"),

    temperature=0,
    reasoning=False,
)

class ChatRequest(BaseModel):
    question: str


def create_event(event: str, data) -> str:
    """데이터를 SSE 형식으로 변환한다."""

    json_data = json.dumps(data, ensure_ascii=False)
    return f"event: {event}\ndata: {json_data}\n\n"


@router.post("")
def ask_question(
    course_id: int,
    request: ChatRequest,
    # [변경] 반환 타입을 dict에서 StreamingResponse로 변경
) -> StreamingResponse:
    """해당 강좌 자료를 검색하고 답변을 스트리밍한다."""

    # course_id가 같은 자료 중 관련 청크 4개 검색
    results = vector_store.similarity_search_with_relevance_scores(
        query=request.question,
        k=4,
        filter={"course_id": course_id},
    )

    # [추가] 관련도 임계값 이상인 청크만 답변 근거로 사용한다.
    results = [
        (document, score)
        for document, score in results
        if score >= MIN_RELEVANCE_SCORE
    ]

    # 검색된 청크를 답변 모델에 전달할 문맥으로 구성
    context = "\n\n".join(document.page_content for document, _ in results)

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

    def generate():
        # [변경] 자료가 없는 경우도 JSON이 아닌 SSE로 반환
        if not results:
            yield create_event(
                "token",
                {"content": REFUSAL},
            )
            yield create_event("sources", [])
            yield create_event("done", {})
            return

        prompt = f"""다음 지시사항과 강의 자료는 내부 참고용이며 답변에 그대로 출력하지 마라.
오직 질문에 대한 최종 답변만 출력해라.

지시사항:
- 강의 자료만 사용해서 질문에 답변해.
- 자료에 답이 없으면 다른 말 없이 정확히 다음 문장만 출력해: {REFUSAL}
- 자료에 관련된 예외나 조건이 있으면 함께 설명해.

강의 자료:
{context}

질문:
{request.question}

답변:"""

        full_answer = ""

        # [변경] invoke 대신 stream을 한 번만 호출
        for chunk in llm.stream(prompt):
            # [추가] 마지막 chunk에 들어오는 Ollama 토큰 사용량 확인
            metadata = chunk.response_metadata

            if metadata.get("done"):
                input_tokens = metadata.get("prompt_eval_count", 0)
                output_tokens = metadata.get("eval_count", 0)
                total_tokens = input_tokens + output_tokens

                # [추가] Prometheus에 토큰 사용량 누적
                LLM_INPUT_TOKENS.inc(input_tokens)
                LLM_OUTPUT_TOKENS.inc(output_tokens)

                logger.info(
                    "LLM token usage - course_id=%s, input=%s, output=%s, total=%s",
                    course_id,
                    input_tokens,
                    output_tokens,
                    total_tokens,
                )

            content = str(chunk.content)

            if not content:
                continue

            full_answer += content

            # 생성된 답변 조각을 즉시 SSE로 전송
            yield create_event(
                "token",
                {"content": content},
            )

        # 모델이 답변을 거절한 경우 출처를 표시하지 않는다.
        final_sources = [] if full_answer.strip() == REFUSAL else sources

        yield create_event("sources", final_sources)
        yield create_event("done", {})

    # [변경] 일반 dict가 아닌 SSE 스트리밍 응답 반환
    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache"},
    )
