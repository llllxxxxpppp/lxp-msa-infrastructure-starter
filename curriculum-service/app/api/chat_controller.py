"""커리큘럼 API 컨트롤러."""

import asyncio
import json
import logging
from collections.abc import AsyncIterator
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Response, status
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage
from pydantic import BaseModel, ConfigDict, Field

from app.api.auth_dependencies import get_authenticated_user_id
from app.services.conversation_session_service import ConversationSessionService

logger = logging.getLogger(__name__)

STREAM_CHUNK_SIZE = 2
STREAM_CHUNK_DELAY_SECONDS = 0.03
REVIEW_PROMPT = (
    "이 커리큘럼이 괜찮으신가요? 바꾸고 싶은 부분이 있다면 말씀해 주세요."
)


class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    message: str = Field(min_length=1, description="사용자 메시지")


class ChatResponse(BaseModel):
    status: str
    message: str
    user_profile: dict[str, str | None]
    target_goal: str | None
    missing_info: list[str]
    curriculum: dict | None


class ChatController:
    """헬스 체크와 커리큘럼 대화 API를 제공합니다."""

    def __init__(
        self,
        session_service: ConversationSessionService,
        ollama_model: str,
        ollama_base_url: str,
        stream_chunk_delay_seconds: float = STREAM_CHUNK_DELAY_SECONDS,
    ) -> None:
        if stream_chunk_delay_seconds < 0:
            raise ValueError("stream_chunk_delay_seconds는 0 이상이어야 합니다.")
        self._session_service = session_service
        self._ollama_model = ollama_model
        self._ollama_base_url = ollama_base_url
        self._stream_chunk_delay_seconds = stream_chunk_delay_seconds
        self.router = APIRouter()
        self.router.add_api_route("/health", self.health, methods=["GET"])
        chat_router = APIRouter(prefix="/api/curriculum")
        chat_router.add_api_route(
            "/chat",
            self.chat,
            methods=["POST"],
            response_model=ChatResponse,
        )
        chat_router.add_api_route(
            "/chat/stream",
            self.chat_stream,
            methods=["POST"],
            response_class=StreamingResponse,
        )
        chat_router.add_api_route(
            "/chat/session",
            self.delete_session,
            methods=["DELETE"],
            status_code=status.HTTP_204_NO_CONTENT,
            response_class=Response,
        )
        self.router.include_router(chat_router)

    async def health(self) -> dict[str, str]:
        return {"status": "ok", "ollama_model": self._ollama_model}

    async def chat(
        self,
        request: ChatRequest,
        user_id: Annotated[int, Depends(get_authenticated_user_id)],
    ) -> ChatResponse:
        return await self._create_chat_response(request, user_id)

    async def chat_stream(
        self,
        request: ChatRequest,
        user_id: Annotated[int, Depends(get_authenticated_user_id)],
    ) -> StreamingResponse:
        async def generate() -> AsyncIterator[str]:
            yield self._create_event("start", {})

            try:
                response = await self._create_chat_response(request, user_id)
            except HTTPException as exc:
                yield self._create_event("error", {"message": str(exc.detail)})
                return

            metadata = response.model_dump(exclude={"message"})
            yield self._create_event("metadata", metadata)

            message = self._display_message(response)
            for offset in range(0, len(message), STREAM_CHUNK_SIZE):
                yield self._create_event(
                    "delta",
                    {"content": message[offset : offset + STREAM_CHUNK_SIZE]},
                )
                if self._stream_chunk_delay_seconds:
                    await asyncio.sleep(self._stream_chunk_delay_seconds)

            yield self._create_event("done", {})

        return StreamingResponse(
            generate(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "X-Accel-Buffering": "no",
            },
        )

    async def _create_chat_response(
        self,
        request: ChatRequest,
        user_id: int,
    ) -> ChatResponse:
        try:
            result = await self._session_service.chat(
                user_id,
                {"messages": [HumanMessage(content=request.message)]},
            )
        except Exception as exc:
            logger.exception("커리큘럼 그래프 실행 중 오류가 발생했습니다.")
            raise HTTPException(
                status_code=503,
                detail=(
                    f"Ollama 모델 호출에 실패했습니다. `{self._ollama_model}` 모델과 "
                    f"`{self._ollama_base_url}` 연결을 확인해 주세요."
                ),
            ) from exc

        last_message = result["messages"][-1]
        return ChatResponse(
            status=result.get("status", "interviewing"),
            message=str(last_message.content),
            user_profile=result.get("user_profile", {}),
            target_goal=result.get("target_goal"),
            missing_info=result.get("missing_info", []),
            curriculum=result.get("draft_curriculum"),
        )

    @staticmethod
    def _create_event(event: str, data: object) -> str:
        payload = json.dumps(data, ensure_ascii=False)
        return f"event: {event}\ndata: {payload}\n\n"

    @staticmethod
    def _display_message(response: ChatResponse) -> str:
        if response.status != "reviewing" or response.curriculum is None:
            return response.message

        lines = [line.strip() for line in response.message.splitlines() if line.strip()]
        return lines[-1] if lines else REVIEW_PROMPT

    async def delete_session(
        self,
        user_id: Annotated[int, Depends(get_authenticated_user_id)],
    ) -> Response:
        await self._session_service.delete(user_id)
        return Response(status_code=status.HTTP_204_NO_CONTENT)
