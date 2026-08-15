#!/usr/bin/env python3

"""LangGraph를 이용한 맞춤형 커리큘럼 설계 API의 간단한 PoC."""

import logging
import os
from json import JSONDecodeError
from typing import Annotated, Literal

from fastapi import FastAPI, HTTPException
from langchain_community.retrievers import BM25Retriever
from langchain_core.documents import Document
from langchain_core.exceptions import OutputParserException
from langchain_core.messages import AIMessage, AnyMessage, HumanMessage, SystemMessage
from langchain_core.runnables import Runnable, RunnableLambda
from langchain_ollama import ChatOllama
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field, ValidationError
from typing_extensions import TypedDict

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen3.5:4b")


COURSES = [
    {
        "id": "data-101",
        "title": "엑셀로 시작하는 데이터 분석",
        "category": "데이터 분석",
        "difficulty": "입문",
        "duration": "4시간",
        "description": "함수, 피벗 테이블, 차트를 활용해 업무 데이터를 정리합니다.",
    },
    {
        "id": "data-201",
        "title": "실무 SQL과 대시보드",
        "category": "데이터 분석",
        "difficulty": "실전",
        "duration": "8시간",
        "description": "SQL로 데이터를 추출하고 핵심 지표 대시보드를 만듭니다.",
    },
    {
        "id": "data-301",
        "title": "Python 기반 예측 분석",
        "category": "데이터 분석",
        "difficulty": "심화",
        "duration": "12시간",
        "description": "Python과 머신러닝으로 예측 모델을 만들고 평가합니다.",
    },
    {
        "id": "marketing-101",
        "title": "디지털 마케팅 입문",
        "category": "마케팅",
        "difficulty": "입문",
        "duration": "3시간",
        "description": "퍼널, 채널, 핵심 마케팅 지표의 기본 개념을 학습합니다.",
    },
    {
        "id": "marketing-201",
        "title": "GA4로 하는 캠페인 성과 분석",
        "category": "마케팅",
        "difficulty": "실전",
        "duration": "6시간",
        "description": "GA4로 캠페인을 분석하고 개선안을 도출합니다.",
    },
    {
        "id": "marketing-301",
        "title": "마케팅 자동화와 실험 설계",
        "category": "마케팅",
        "difficulty": "심화",
        "duration": "9시간",
        "description": "고객 세분화, 자동화 시나리오, A/B 테스트를 설계합니다.",
    },
    {
        "id": "backend-101",
        "title": "웹과 REST API 기초",
        "category": "백엔드 개발",
        "difficulty": "입문",
        "duration": "5시간",
        "description": "HTTP, REST, 데이터베이스 등 백엔드의 기초를 학습합니다.",
    },
    {
        "id": "backend-201",
        "title": "FastAPI 실전 프로젝트",
        "category": "백엔드 개발",
        "difficulty": "실전",
        "duration": "10시간",
        "description": "FastAPI로 인증과 데이터베이스를 포함한 API를 구현합니다.",
    },
    {
        "id": "backend-301",
        "title": "확장 가능한 백엔드 아키텍처",
        "category": "백엔드 개발",
        "difficulty": "심화",
        "duration": "12시간",
        "description": "캐시, 메시지 큐, 관측성을 적용해 서비스를 확장합니다.",
    },
    {
        "id": "product-101",
        "title": "프로덕트 매니지먼트 기초",
        "category": "프로덕트",
        "difficulty": "입문",
        "duration": "4시간",
        "description": "고객 문제 정의부터 제품 지표까지 기본기를 익힙니다.",
    },
    {
        "id": "product-201",
        "title": "데이터 기반 제품 개선",
        "category": "프로덕트",
        "difficulty": "실전",
        "duration": "7시간",
        "description": "제품 데이터를 분석하고 가설과 실험을 설계합니다.",
    },
    {
        "id": "product-301",
        "title": "제품 전략과 로드맵",
        "category": "프로덕트",
        "difficulty": "심화",
        "duration": "8시간",
        "description": "사업 목표와 고객 가치를 연결한 제품 전략을 수립합니다.",
    },
]


def _course_document(course: dict[str, str]) -> Document:
    content = (
        f"{course['title']} {course['category']} {course['difficulty']} "
        f"{course['description']} 소요 시간 {course['duration']}"
    )
    return Document(page_content=content, metadata=course)


COURSE_DOCUMENTS = [_course_document(course) for course in COURSES]
retriever = BM25Retriever.from_documents(
    COURSE_DOCUMENTS,
    preprocess_func=lambda text: text.lower().split(),
)
retriever.k = 8

llm = ChatOllama(
    model=OLLAMA_MODEL,
    base_url=OLLAMA_BASE_URL,
    temperature=0,
    reasoning=False,
    num_predict=2048,
)


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


STRUCTURED_OUTPUT_EXCEPTIONS = (
    OutputParserException,
    ValidationError,
    JSONDecodeError,
)


def _unwrap_structured_response(response: dict, schema_name: str) -> BaseModel:
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


def _create_structured_llm(schema: type[BaseModel]) -> Runnable:
    structured_llm = llm.with_structured_output(
        schema,
        method="json_schema",
        include_raw=True,
    )
    parser = RunnableLambda(
        lambda response: _unwrap_structured_response(response, schema.__name__)
    )
    return (structured_llm | parser).with_retry(
        retry_if_exception_type=STRUCTURED_OUTPUT_EXCEPTIONS,
        stop_after_attempt=3,
    )


interviewer_llm = _create_structured_llm(InterviewResult)
feedback_llm = _create_structured_llm(FeedbackResult)
planner_llm = _create_structured_llm(CurriculumPlan)


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


def _conversation_text(messages: list[AnyMessage]) -> str:
    lines = []
    for message in messages:
        role = "사용자" if isinstance(message, HumanMessage) else "컨설턴트"
        lines.append(f"{role}: {message.content}")
    return "\n".join(lines)


def _missing_profile_info(
    profile: dict[str, str | None],
    target_goal: str | None,
) -> list[str]:
    required = {
        "job": profile.get("job"),
        "current_level": profile.get("current_level"),
        "target_goal": target_goal,
    }
    return [key for key, value in required.items() if not value]


def _question_for(missing_info: str) -> str:
    return {
        "job": "현재 어떤 직무를 맡고 계신가요?",
        "current_level": "현재 관련 역량 수준은 어느 정도인가요?",
        "target_goal": "이번 학습을 통해 어떤 목표를 달성하고 싶으신가요?",
    }[missing_info]


async def interviewer_node(state: CurriculumState) -> dict:
    current_profile = state.get("user_profile", {})
    current_goal = state.get("target_goal")
    try:
        result = await interviewer_llm.ainvoke(
            [
                SystemMessage(
                    content=(
                        "당신은 사내 LXP의 학습 컨설턴트입니다. 대화에서 사용자가 "
                        "명시한 직무, 경력, 현재 역량 수준, 학습 목표를 추출하세요. "
                        "기존에 파악된 정보는 유지하고 추측하지 마세요. 직무, 현재 "
                        "수준, 목표 중 빠진 정보가 있으면 한 번에 하나만 정중한 "
                        "한국어로 질문하세요."
                    )
                ),
                HumanMessage(
                    content=(
                        f"기존 프로필: {current_profile}\n"
                        f"기존 목표: {current_goal}\n"
                        f"대화:\n{_conversation_text(state['messages'])}"
                    )
                ),
            ]
        )
    except STRUCTURED_OUTPUT_EXCEPTIONS:
        logger.exception(
            "인터뷰 결과 파싱 재시도가 모두 실패해 기존 상태로 질문을 생성합니다."
        )
        result = InterviewResult(
            user_profile=UserProfile(),
            target_goal=None,
            next_question=None,
        )

    extracted = result.user_profile.model_dump()
    merged_profile = {
        key: extracted.get(key) or current_profile.get(key)
        for key in ("job", "experience", "current_level")
    }
    target_goal = result.target_goal or current_goal
    missing_info = _missing_profile_info(merged_profile, target_goal)

    update: dict = {
        "user_profile": merged_profile,
        "target_goal": target_goal,
        "missing_info": missing_info,
    }
    if missing_info:
        question = result.next_question or _question_for(missing_info[0])
        update.update(messages=[AIMessage(content=question)], status="interviewing")
    return update


async def retrieve_node(state: CurriculumState) -> dict:
    profile = state["user_profile"]
    query = " ".join(
        value
        for value in (
            profile.get("job"),
            profile.get("current_level"),
            state.get("target_goal"),
            state.get("feedback"),
        )
        if value
    )

    candidates: list[dict[str, str]] = []
    seen_ids: set[str] = set()
    for stage in ("입문", "실전", "심화"):
        ranked = retriever.invoke(f"{query} {stage}")
        stage_documents = [doc for doc in ranked if doc.metadata["difficulty"] == stage]
        if not stage_documents:
            stage_documents = [
                doc for doc in COURSE_DOCUMENTS if doc.metadata["difficulty"] == stage
            ]
        for document in stage_documents[:2]:
            course = dict(document.metadata)
            if course["id"] not in seen_ids:
                candidates.append(course)
                seen_ids.add(course["id"])
    return {"retrieved_courses": candidates}


def _normalize_plan(plan: CurriculumPlan, candidates: list[dict[str, str]]) -> dict:
    candidate_by_id = {course["id"]: course for course in candidates}
    generated_by_stage = {step.stage: step for step in plan.steps}
    normalized_steps = []

    for stage in ("입문", "실전", "심화"):
        generated = generated_by_stage.get(stage)
        course = candidate_by_id.get(generated.course_id) if generated else None
        if not course or course["difficulty"] != stage:
            course = next(item for item in candidates if item["difficulty"] == stage)
        normalized_steps.append(
            {
                "stage": stage,
                "course_id": course["id"],
                "title": course["title"],
                "duration": course["duration"],
                "reason": generated.reason
                if generated
                else "앞 단계에서 익힌 내용을 자연스럽게 확장할 수 있는 강의입니다.",
            }
        )
    return {"summary": plan.summary, "steps": normalized_steps}


def _render_curriculum(curriculum: dict) -> str:
    lines = [curriculum["summary"], ""]
    for step in curriculum["steps"]:
        lines.append(
            f"- {step['stage']}: {step['title']} ({step['duration']})\n"
            f"  추천 이유: {step['reason']}"
        )
    lines.append("\n이 커리큘럼이 괜찮으신가요? 바꾸고 싶은 부분이 있다면 말씀해 주세요.")
    return "\n".join(lines)


def _fallback_curriculum(state: CurriculumState) -> dict:
    candidates = state.get("retrieved_courses", [])
    fallback_reasons = {
        "입문": "목표 달성에 필요한 기본 개념을 먼저 익힐 수 있는 강의입니다.",
        "실전": "기초 내용을 실제 업무에 적용하는 방법을 연습할 수 있습니다.",
        "심화": "앞 단계의 학습 내용을 확장해 독립적으로 문제를 해결할 수 있습니다.",
    }
    steps = []
    for stage in ("입문", "실전", "심화"):
        course = next(
            (item for item in candidates if item["difficulty"] == stage),
            next(item for item in COURSES if item["difficulty"] == stage),
        )
        steps.append(
            {
                "stage": stage,
                "course_id": course["id"],
                "title": course["title"],
                "duration": course["duration"],
                "reason": fallback_reasons[stage],
            }
        )

    target_goal = state.get("target_goal") or "학습 목표"
    return {
        "summary": f"{target_goal} 달성을 위해 기초부터 심화까지 단계적으로 구성했습니다.",
        "steps": steps,
    }


async def planner_node(state: CurriculumState) -> dict:
    try:
        plan = await planner_llm.ainvoke(
            [
                SystemMessage(
                    content=(
                        "당신은 교육 커리큘럼 기획자입니다. 후보 강의만 사용하여 입문, "
                        "실전, 심화 순서로 각 한 강의씩 선택하세요. course_id는 반드시 "
                        "후보에 있는 값을 그대로 사용하고, 추천 이유와 요약은 정중한 "
                        "한국어로 작성하세요. 사용자 피드백이 있다면 반영하세요."
                    )
                ),
                HumanMessage(
                    content=(
                        f"사용자 프로필: {state['user_profile']}\n"
                        f"학습 목표: {state['target_goal']}\n"
                        f"피드백: {state.get('feedback', '')}\n"
                        f"후보 강의: {state['retrieved_courses']}"
                    )
                ),
            ]
        )
        curriculum = _normalize_plan(plan, state["retrieved_courses"])
    except STRUCTURED_OUTPUT_EXCEPTIONS:
        logger.exception(
            "커리큘럼 파싱 재시도가 모두 실패해 기본 커리큘럼을 생성합니다."
        )
        curriculum = _fallback_curriculum(state)
    return {
        "draft_curriculum": curriculum,
        "messages": [AIMessage(content=_render_curriculum(curriculum))],
        "status": "reviewing",
    }


async def feedback_node(state: CurriculumState) -> dict:
    latest_message = str(state["messages"][-1].content)
    try:
        result = await feedback_llm.ainvoke(
            [
                SystemMessage(
                    content=(
                        "사용자의 커리큘럼 피드백을 분류하세요. 만족하거나 동의하면 "
                        "approve, 추천 이유나 순서처럼 기존 후보 안에서 수정 가능하면 "
                        "replan, 더 짧은 강의나 다른 주제처럼 강의를 다시 찾아야 하면 "
                        "retrieve입니다."
                    )
                ),
                HumanMessage(content=latest_message),
            ]
        )
    except STRUCTURED_OUTPUT_EXCEPTIONS:
        logger.exception(
            "피드백 파싱 재시도가 모두 실패해 규칙 기반으로 분류합니다."
        )
        normalized_message = "".join(latest_message.lower().split())
        negative_phrases = ("안좋", "별로", "수정", "바꿔", "변경", "다른")
        approval_phrases = ("좋아", "좋습니다", "괜찮", "동의", "확정", "진행")
        is_approval = not any(
            phrase in normalized_message for phrase in negative_phrases
        ) and any(phrase in normalized_message for phrase in approval_phrases)
        result = FeedbackResult(
            action="approve" if is_approval else "replan",
            feedback=latest_message,
        )
    if result.action == "approve":
        return {
            "feedback": result.feedback,
            "feedback_action": result.action,
            "messages": [
                AIMessage(content="좋습니다. 이 커리큘럼을 최종 학습 로드맵으로 확정하겠습니다.")
            ],
            "status": "completed",
        }
    return {
        "feedback": result.feedback,
        "feedback_action": result.action,
        "status": "reviewing",
    }


async def route_from_start(state: CurriculumState) -> str:
    return "feedback" if state.get("draft_curriculum") else "interviewer"


async def route_after_interview(state: CurriculumState) -> str:
    return "end" if state.get("missing_info") else "retrieve"


async def route_after_feedback(state: CurriculumState) -> str:
    if state.get("status") == "completed":
        return END
    return "retrieve" if state.get("feedback_action") == "retrieve" else "planner"


graph_builder = StateGraph(CurriculumState)
graph_builder.add_node("interviewer", interviewer_node)
graph_builder.add_node("retrieve", retrieve_node)
graph_builder.add_node("planner", planner_node)
graph_builder.add_node("feedback", feedback_node)
graph_builder.add_conditional_edges(
    START,
    route_from_start,
    {"feedback": "feedback", "interviewer": "interviewer"},
)
graph_builder.add_conditional_edges(
    "interviewer",
    route_after_interview,
    {"end": END, "retrieve": "retrieve"},
)
graph_builder.add_edge("retrieve", "planner")
graph_builder.add_conditional_edges(
    "feedback",
    route_after_feedback,
    {"__end__": END, "planner": "planner", "retrieve": "retrieve"},
)
graph_builder.add_edge("planner", END)
graph = graph_builder.compile(checkpointer=InMemorySaver())


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

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
