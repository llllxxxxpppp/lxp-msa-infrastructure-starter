"""커리큘럼 API 컨트롤러."""

import logging
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Response, status
from langchain_core.messages import HumanMessage
from pydantic import BaseModel, ConfigDict, Field

from app.api.auth_dependencies import get_authenticated_user_id
from app.services.conversation_session_service import ConversationSessionService

logger = logging.getLogger(__name__)


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
    ) -> None:
        self._session_service = session_service
        self._ollama_model = ollama_model
        self._ollama_base_url = ollama_base_url
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

    async def delete_session(
        self,
        user_id: Annotated[int, Depends(get_authenticated_user_id)],
    ) -> Response:
        await self._session_service.delete(user_id)
        return Response(status_code=status.HTTP_204_NO_CONTENT)
