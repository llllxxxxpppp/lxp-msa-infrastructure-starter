from fastapi import FastAPI

from app.chat import router as chat_router
from app.documents import router as documents_router

# AI 챗봇 FastAPI 애플리케이션 생성
app = FastAPI(title="LXP AI Bot Service")

# PDF 업로드·목록·삭제 API 등록
app.include_router(documents_router)

# RAG 질문 API
app.include_router(chat_router)
 
@app.get("/health")
async def health() -> dict[str, str]:
    """AI Bot 서비스 실행 상태를 확인한다."""

    return {"status": "UP"}