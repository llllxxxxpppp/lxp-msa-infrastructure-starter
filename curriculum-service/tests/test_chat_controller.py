"""사용자 기반 채팅과 세션 초기화 API 테스트."""

from typing import Annotated
from unittest import TestCase
from unittest.mock import AsyncMock, Mock

from fastapi import FastAPI
from fastapi.testclient import TestClient
from langchain_core.messages import AIMessage, AnyMessage, HumanMessage
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from typing_extensions import TypedDict

from app.api.chat_controller import ChatController
from app.services.conversation_session_service import ConversationSessionService


def _result(message: str) -> dict:
    return {
        "messages": [AIMessage(content=message)],
        "status": "interviewing",
        "user_profile": {},
        "missing_info": [],
    }


def _client(session_service) -> TestClient:
    controller = ChatController(
        session_service=session_service,
        ollama_model="test-model",
        ollama_base_url="http://ollama.test",
    )
    app = FastAPI()
    app.include_router(controller.router)
    return TestClient(app)


class ChatControllerApiTest(TestCase):
    def setUp(self) -> None:
        self.session_service = Mock()
        self.session_service.chat = AsyncMock(return_value=_result("응답"))
        self.session_service.delete = AsyncMock()
        self.client = _client(self.session_service)

    def test_chat_uses_header_user_and_ignores_body_thread_id(self) -> None:
        response = self.client.post(
            "/api/curriculum/chat",
            headers={"X-User-Id": " 7 "},
            json={"thread_id": "다른 사용자", "message": "안녕하세요"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertNotIn("thread_id", response.json())
        self.assertEqual(response.json()["message"], "응답")
        call = self.session_service.chat.await_args
        self.assertEqual(call.args[0], 7)
        self.assertEqual(call.args[1]["messages"][0].content, "안녕하세요")

    def test_chat_requires_message(self) -> None:
        response = self.client.post(
            "/api/curriculum/chat",
            headers={"X-User-Id": "1"},
            json={"thread_id": "1"},
        )

        self.assertEqual(response.status_code, 422)
        self.session_service.chat.assert_not_awaited()

    def test_invalid_user_id_returns_401_without_calling_session(self) -> None:
        for user_id in (None, "", "0", "invalid"):
            with self.subTest(user_id=user_id):
                self.session_service.chat.reset_mock()
                headers = {}
                if user_id is not None:
                    headers["X-User-Id"] = user_id

                response = self.client.post(
                    "/api/curriculum/chat",
                    headers=headers,
                    json={"message": "안녕하세요"},
                )

                self.assertEqual(response.status_code, 401)
                self.session_service.chat.assert_not_awaited()

    def test_delete_session_is_idempotent_and_returns_empty_204(self) -> None:
        for _ in range(2):
            response = self.client.delete(
                "/api/curriculum/chat/session",
                headers={"X-User-Id": "1"},
            )

            self.assertEqual(response.status_code, 204)
            self.assertEqual(response.content, b"")

        self.assertEqual(self.session_service.delete.await_count, 2)
        self.session_service.delete.assert_awaited_with(1)

    def test_delete_session_requires_authenticated_user(self) -> None:
        response = self.client.delete("/api/curriculum/chat/session")

        self.assertEqual(response.status_code, 401)
        self.session_service.delete.assert_not_awaited()


class ConversationState(TypedDict):
    messages: Annotated[list[AnyMessage], add_messages]


class FakeClock:
    def __init__(self) -> None:
        self.now = 0.0

    def __call__(self) -> float:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += seconds


def _stateful_session_service(clock: FakeClock) -> ConversationSessionService:
    async def respond(state: ConversationState) -> dict:
        turn_count = sum(
            isinstance(message, HumanMessage) for message in state["messages"]
        )
        return {"messages": [AIMessage(content=f"{turn_count}번째 메시지")]}

    graph_builder = StateGraph(ConversationState)
    graph_builder.add_node("respond", respond)
    graph_builder.add_edge(START, "respond")
    graph_builder.add_edge("respond", END)
    checkpointer = InMemorySaver()
    graph = graph_builder.compile(checkpointer=checkpointer)
    return ConversationSessionService(
        graph=graph,
        checkpointer=checkpointer,
        timeout_seconds=30,
        cleanup_interval_seconds=10,
        clock=clock,
    )


class ChatSessionIntegrationTest(TestCase):
    def setUp(self) -> None:
        self.clock = FakeClock()
        self.client = _client(_stateful_session_service(self.clock))

    def _chat(self, user_id: int) -> str:
        response = self.client.post(
            "/api/curriculum/chat",
            headers={"X-User-Id": str(user_id)},
            json={"message": "메시지"},
        )
        self.assertEqual(response.status_code, 200)
        return response.json()["message"]

    def test_conversation_state_is_kept_per_user(self) -> None:
        self.assertEqual(self._chat(1), "1번째 메시지")
        self.assertEqual(self._chat(1), "2번째 메시지")
        self.assertEqual(self._chat(2), "1번째 메시지")

    def test_explicit_reset_starts_new_conversation(self) -> None:
        self.assertEqual(self._chat(1), "1번째 메시지")

        response = self.client.delete(
            "/api/curriculum/chat/session",
            headers={"X-User-Id": "1"},
        )
        repeated_response = self.client.delete(
            "/api/curriculum/chat/session",
            headers={"X-User-Id": "1"},
        )

        self.assertEqual(response.status_code, 204)
        self.assertEqual(repeated_response.status_code, 204)
        self.assertEqual(self._chat(1), "1번째 메시지")

    def test_expired_session_starts_new_conversation_without_error(self) -> None:
        self.assertEqual(self._chat(1), "1번째 메시지")
        self.clock.advance(30)

        self.assertEqual(self._chat(1), "1번째 메시지")
