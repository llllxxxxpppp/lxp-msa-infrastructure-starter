"""사용자별 인메모리 대화 세션의 수명 주기를 관리합니다."""

import asyncio
import time
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from typing import Protocol

from langgraph.checkpoint.memory import InMemorySaver


class CompiledConversationGraph(Protocol):
    """세션 서비스가 호출하는 컴파일된 LangGraph 인터페이스입니다."""

    async def ainvoke(
        self,
        input_state: dict[str, object],
        config: dict[str, dict[str, str]],
    ) -> dict:
        """주어진 대화 상태로 그래프를 실행합니다."""


@dataclass
class _SessionMetadata:
    lock: asyncio.Lock = field(default_factory=asyncio.Lock)
    last_activity: float | None = None
    is_running: bool = False


class ConversationSessionService:
    """사용자별 그래프 실행을 직렬화하고 비활성 세션을 정리합니다."""

    def __init__(
        self,
        graph: CompiledConversationGraph,
        checkpointer: InMemorySaver,
        timeout_seconds: int,
        cleanup_interval_seconds: int,
        *,
        clock: Callable[[], float] = time.monotonic,
        sleep: Callable[[float], Awaitable[None]] = asyncio.sleep,
    ) -> None:
        if timeout_seconds <= 0:
            raise ValueError("timeout_seconds는 양의 정수여야 합니다.")
        if cleanup_interval_seconds <= 0:
            raise ValueError("cleanup_interval_seconds는 양의 정수여야 합니다.")

        self._graph = graph
        self._checkpointer = checkpointer
        self._timeout_seconds = timeout_seconds
        self._cleanup_interval_seconds = cleanup_interval_seconds
        self._clock = clock
        self._sleep = sleep
        self._sessions: dict[str, _SessionMetadata] = {}
        self._sessions_lock = asyncio.Lock()

    async def chat(
        self,
        user_id: int,
        input_state: dict[str, object],
    ) -> dict:
        """사용자의 대화를 이어서 실행하고 마지막 활동 시각을 기록합니다."""

        thread_id = str(user_id)
        session = await self._acquire_current_session(thread_id)
        try:
            if self._is_expired(session, self._clock()):
                await self._checkpointer.adelete_thread(thread_id)
                replacement = _SessionMetadata()
                await replacement.lock.acquire()
                async with self._sessions_lock:
                    self._sessions[thread_id] = replacement
                session.lock.release()
                session = replacement

            session.is_running = True
            try:
                return await self._graph.ainvoke(
                    input_state,
                    config={"configurable": {"thread_id": thread_id}},
                )
            finally:
                session.is_running = False
                session.last_activity = self._clock()
        finally:
            session.lock.release()

    async def delete(self, user_id: int) -> None:
        """사용자의 체크포인트와 세션 메타데이터를 멱등하게 제거합니다."""

        thread_id = str(user_id)
        session = await self._acquire_current_session(thread_id)
        try:
            await self._checkpointer.adelete_thread(thread_id)
            async with self._sessions_lock:
                if self._sessions.get(thread_id) is session:
                    del self._sessions[thread_id]
        finally:
            session.lock.release()

    async def cleanup_expired(self) -> None:
        """현재 실행 중이지 않은 만료 세션을 정리합니다."""

        async with self._sessions_lock:
            sessions = tuple(self._sessions.items())

        for thread_id, session in sessions:
            if session.is_running or session.lock.locked():
                continue

            await session.lock.acquire()
            try:
                async with self._sessions_lock:
                    if self._sessions.get(thread_id) is not session:
                        continue

                if session.is_running or not self._is_expired(
                    session,
                    self._clock(),
                ):
                    continue

                await self._checkpointer.adelete_thread(thread_id)
                async with self._sessions_lock:
                    if self._sessions.get(thread_id) is session:
                        del self._sessions[thread_id]
            finally:
                session.lock.release()

    async def run_cleanup(self) -> None:
        """설정된 주기로 만료 세션을 계속 정리합니다."""

        while True:
            await self._sleep(self._cleanup_interval_seconds)
            await self.cleanup_expired()

    async def _acquire_current_session(self, thread_id: str) -> _SessionMetadata:
        while True:
            async with self._sessions_lock:
                session = self._sessions.get(thread_id)
                if session is None:
                    session = _SessionMetadata()
                    self._sessions[thread_id] = session

            await session.lock.acquire()
            async with self._sessions_lock:
                if self._sessions.get(thread_id) is session:
                    return session
            session.lock.release()

    def _is_expired(self, session: _SessionMetadata, now: float) -> bool:
        return (
            session.last_activity is not None
            and now - session.last_activity >= self._timeout_seconds
        )
