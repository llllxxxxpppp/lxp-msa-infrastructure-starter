"""Ollama 및 구조화 출력 서비스."""

import logging
from json import JSONDecodeError
from typing import Literal

from langchain_core.exceptions import OutputParserException
from langchain_core.messages import BaseMessage
from langchain_core.runnables import Runnable, RunnableLambda
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field, ValidationError

logger = logging.getLogger(__name__)


class UserProfile(BaseModel):
    job: str | None = Field(default=None, description="사용자의 직무")
    experience: str | None = Field(default=None, description="사용자의 연차 또는 경력")
    current_level: str | None = Field(default=None, description="현재 역량 수준")


class InterviewResult(BaseModel):
    user_profile: UserProfile
    target_goal: str | None = Field(default=None, description="달성하려는 학습 목표")
    next_question: str | None = Field(
        default=None,
        description="부족한 정보 하나를 확인하는 정중한 한국어 질문",
    )


class FeedbackResult(BaseModel):
    action: Literal["approve", "replan", "retrieve"] = Field(
        description="승인은 approve, 기존 강의 내 수정은 replan, 다른 강의 검색은 retrieve"
    )
    feedback: str = Field(default="", description="사용자의 수정 요구사항 요약")


class CurriculumStep(BaseModel):
    stage: Literal["입문", "실전", "심화"]
    course_id: str
    reason: str = Field(description="사용자에게 이 강의를 추천하는 이유")


class CurriculumPlan(BaseModel):
    summary: str = Field(description="전체 학습 방향을 설명하는 한두 문장")
    steps: list[CurriculumStep]


class LlmService:
    """워크플로우에서 사용하는 구조화 LLM 호출을 제공합니다."""

    structured_output_exceptions = (
        OutputParserException,
        ValidationError,
        JSONDecodeError,
    )

    def __init__(self, model: str, base_url: str) -> None:
        self._llm = ChatOllama(
            model=model,
            base_url=base_url,
            temperature=0,
            reasoning=False,
            num_predict=2048,
        )
        self._interviewer = self._create_structured_llm(InterviewResult)
        self._feedback = self._create_structured_llm(FeedbackResult)
        self._planner = self._create_structured_llm(CurriculumPlan)

    @staticmethod
    def _unwrap_structured_response(
        response: dict,
        schema_name: str,
    ) -> BaseModel:
        parsing_error = response.get("parsing_error")
        if parsing_error is not None:
            raw_message = response.get("raw")
            raw_content = str(getattr(raw_message, "content", ""))[:1000]
            logger.warning(
                "%s 구조화 출력 파싱에 실패했습니다. error=%r raw=%r",
                schema_name,
                parsing_error,
                raw_content,
            )
            raise parsing_error

        parsed = response.get("parsed")
        if parsed is None:
            raise OutputParserException(f"{schema_name} 구조화 출력이 비어 있습니다.")
        return parsed

    def _create_structured_llm(self, schema: type[BaseModel]) -> Runnable:
        structured_llm = self._llm.with_structured_output(
            schema,
            method="json_schema",
            include_raw=True,
        )
        parser = RunnableLambda(
            lambda response: self._unwrap_structured_response(
                response,
                schema.__name__,
            )
        )
        return (structured_llm | parser).with_retry(
            retry_if_exception_type=self.structured_output_exceptions,
            stop_after_attempt=3,
        )

    async def interview(self, messages: list[BaseMessage]) -> InterviewResult:
        return await self._interviewer.ainvoke(messages)

    async def classify_feedback(self, messages: list[BaseMessage]) -> FeedbackResult:
        return await self._feedback.ainvoke(messages)

    async def create_plan(self, messages: list[BaseMessage]) -> CurriculumPlan:
        return await self._planner.ainvoke(messages)
