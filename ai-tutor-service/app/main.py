from fastapi import FastAPI

from app.chat import router as chat_router
from app.documents import index_demo_pdfs, router as documents_router

from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
from fastapi.responses import Response

# AI 챗봇 FastAPI 애플리케이션 생성
app = FastAPI(title="LXP AI Tutor Service")

# PDF 업로드·목록·삭제 API 등록
app.include_router(documents_router)

# RAG 질문 API
app.include_router(chat_router)


# [추가] 서비스 시작 시 발표용 demo PDF 자동 등록
@app.on_event("startup")
def startup_event() -> None:
    index_demo_pdfs()


@app.get("/health")
async def health() -> dict[str, str]:
    """AI Tutor 서비스 실행 상태를 확인한다."""

    return {"status": "UP"}

@app.get("/metrics", include_in_schema=False)
def metrics():
    return Response(
        content=generate_latest(),
        media_type=CONTENT_TYPE_LATEST,
    )