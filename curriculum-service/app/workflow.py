"""맞춤형 커리큘럼 LangGraph 워크플로우."""

import logging

from langchain_core.messages import AIMessage, AnyMessage, HumanMessage, SystemMessage
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph

from app.courses import COURSES, COURSE_DOCUMENTS, search_courses
from app.llm import (
    STRUCTURED_OUTPUT_EXCEPTIONS,
    feedback_llm,
    interviewer_llm,
    planner_llm,
)
from app.models import (
    CurriculumPlan,
    CurriculumState,
    FeedbackResult,
    InterviewResult,
    UserProfile,
)

logger = logging.getLogger(__name__)


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
        stage_documents = search_courses(query, difficulty=stage)
        if not stage_documents:
            stage_documents = [
                doc for doc in COURSE_DOCUMENTS if doc.metadata["difficulty"] == stage
            ]
        for document in stage_documents:
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
