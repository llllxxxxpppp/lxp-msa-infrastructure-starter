"""API 및 커리큘럼 워크플로우에서 사용하는 데이터 모델."""

from typing import Annotated, Literal

from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field
from typing_extensions import TypedDict


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


class CurriculumState(TypedDict, total=False):
    messages: Annotated[list[AnyMessage], add_messages]
    user_profile: dict[str, str | None]
    target_goal: str | None
    missing_info: list[str]
    retrieved_courses: list[dict[str, str]]
    draft_curriculum: dict
    feedback: str
    feedback_action: Literal["approve", "replan", "retrieve"]
    status: Literal["interviewing", "reviewing", "completed"]


class ChatRequest(BaseModel):
    thread_id: str = Field(min_length=1, description="대화 상태를 구분하는 식별자")
    message: str = Field(min_length=1, description="사용자 메시지")


class ChatResponse(BaseModel):
    thread_id: str
    status: str
    message: str
    user_profile: dict[str, str | None]
    target_goal: str | None
    missing_info: list[str]
    curriculum: dict | None
