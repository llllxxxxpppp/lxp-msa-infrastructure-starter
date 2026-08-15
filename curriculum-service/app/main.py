#!/usr/bin/env python3

"""LangGraph를 이용한 맞춤형 커리큘럼 설계 API."""

import logging

from fastapi import FastAPI, HTTPException
from langchain_core.messages import HumanMessage

from app.config import OLLAMA_BASE_URL, OLLAMA_MODEL
from app.models import ChatRequest, ChatResponse
from app.workflow import graph

logger = logging.getLogger(__name__)

app = FastAPI(title="맞춤형 커리큘럼 설계 봇", version="0.1.0")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "ollama_model": OLLAMA_MODEL}


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    try:
        result = await graph.ainvoke(
            {"messages": [HumanMessage(content=request.message)]},
            config={"configurable": {"thread_id": request.thread_id}},
        )
    except Exception as exc:
        logger.exception("커리큘럼 그래프 실행 중 오류가 발생했습니다.")
        raise HTTPException(
            status_code=503,
            detail=(
                f"Ollama 모델 호출에 실패했습니다. `{OLLAMA_MODEL}` 모델과 "
                f"`{OLLAMA_BASE_URL}` 연결을 확인해 주세요."
            ),
        ) from exc

    last_message = result["messages"][-1]
    return ChatResponse(
        thread_id=request.thread_id,
        status=result.get("status", "interviewing"),
        message=str(last_message.content),
        user_profile=result.get("user_profile", {}),
        target_goal=result.get("target_goal"),
        missing_info=result.get("missing_info", []),
        curriculum=result.get("draft_curriculum"),
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
