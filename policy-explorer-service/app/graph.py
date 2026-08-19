"""규정 충돌 검출 파이프라인 (LangGraph).

PoC 리포(policy-explorer-service)의 `lxp-ollama-qwen-fileupload.py` 중
"4~6. LangGraph 상태/노드/그래프 빌드"를 옮긴 모듈이다. 4단계 구조와 프롬프트는 원본 그대로다.

    ① extract_rules      LLM이 신규 문서에서 핵심 규정(팩트)을 구조화 추출
    ② retrieve_legacy    하이브리드 검색(Chroma 벡터 + BM25 키워드)으로 기존 문서 조회
    ③ analyze_conflicts  LLM이 기존 내용과 신규 팩트를 비교해 충돌 여부 판단
    ④ generate_report    충돌 항목만 모아 마크다운 표 리포트 생성 (LLM 미사용)

원본과 달라진 점
  - `ChatOllama`에 `base_url`을 명시한다. 원본은 인자가 없어 langchain-ollama 기본값인
    `http://localhost:11434`를 썼는데, 컨테이너 안에서 localhost는 컨테이너 자신이라
    호스트 Ollama를 절대 찾지 못한다. 이 한 줄이 컨테이너<->Ollama 통신의 관문이다.
  - 그래프 조립 시점을 import에서 build_graph() 호출로 옮겼다.

채택 근거(왜 LangGraph인지, 왜 한국어 강제 지시문이 있는지)는 PoC 리포 `select_reason.md` 2·6절.
"""

import logging
import time
from typing import Dict, List, TypedDict

from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
from langgraph.graph import END, StateGraph
from pydantic import BaseModel, Field

from app import config
from app.rag import RagStore

logger = logging.getLogger(__name__)


# ---------------------------------------------------------
# LLM 구조화 출력 스키마
# ---------------------------------------------------------
class ExtractedRule(BaseModel):
    keyword: str = Field(description="규정의 핵심 검색 키워드 (예: 반차, 경조사 휴가, 중식대)")
    fact: str = Field(description="새로운 규정의 명제 팩트 (예: 반차 사용 기준 시간 4.5시간으로 변경)")


class RuleExtractionOutput(BaseModel):
    rules: List[ExtractedRule]


class ConflictAnalysis(BaseModel):
    is_conflict: bool = Field(description="기존 내용과 신규 규정이 충돌(불일치)하면 true, 아니면 false")
    # 🚨 Qwen/EXAONE 모두 구조화 출력에서 중국어가 섞여 나오는 이슈가 있어 한국어를 강제한다.
    # action_suggested/reasoning을 분리한 이유: 이전에는 결론과 판단 근거가 한 필드에 섞여 나와
    # UI에서 긴 서술형 문단 하나로 뭉쳐 보였다(가독성 저하 — 실제 사용자 화면에서 재현됨).
    action_suggested: str = Field(
        description=(
            "수정 제안 — 반드시 한 문장, 조치만 명시할 것. 이유·설명은 절대 포함하지 말 것"
            "(그건 reasoning 필드에 쓴다). 충돌 시에만 작성하며 반드시 '한국어(Korean)'로만"
            " 작성할 것. 예: '5일을 7일로 변경 권장'"
        )
    )
    reasoning: str = Field(
        description=(
            "왜 충돌로 판단했는지의 근거를 서술할 것. action_suggested와 달리 여러 문장으로"
            " 자유롭게 설명해도 된다. 반드시 '한국어(Korean)'로만 작성할 것."
        )
    )


# ---------------------------------------------------------
# 그래프 상태
# ---------------------------------------------------------
class GraphState(TypedDict):
    new_policy_doc: str
    extracted_rules: List[Dict]
    search_results: List[Dict]
    conflict_report: List[Dict]
    final_markdown_report: str


def _label() -> str:
    """로그 앞에 붙는 [엔진 | 모델] 라벨."""
    return f"[{config.CURRENT_ENGINE} | {config.CURRENT_MODEL}]"


def build_graph(store: RagStore):
    """생성 LLM을 만들고 4단계 그래프를 조립해 컴파일된 그래프를 반환한다."""

    # 🚨 base_url이 핵심. 원본에는 이 인자가 없었다.
    llm = ChatOllama(
        model=config.OLLAMA_MODEL,
        base_url=config.OLLAMA_BASE_URL,
        temperature=0.0,
        reasoning=False,    # 추론 on/off
    )
    rule_extractor_llm = llm.with_structured_output(RuleExtractionOutput)
    structured_llm = llm.with_structured_output(ConflictAnalysis)

    # -----------------------------------------------------
    # ① 규정 팩트 추출
    # -----------------------------------------------------
    def extract_rules_node(state: GraphState) -> Dict:
        logger.info("%s [Node 1] 규정 팩트 추출 시작...", _label())
        start_t = time.time()

        prompt = ChatPromptTemplate.from_template(
            "다음은 새롭게 개정된 사내 규정 문서입니다. 이 문서에서 변경된 핵심 규정(팩트)들을 추출하세요.\n\n"
            "문서: {policy}"
        )
        result = (prompt | rule_extractor_llm).invoke({"policy": state["new_policy_doc"]})

        logger.info(
            "%s 👉 [Node 1 완료] 추출 소요 시간: %.2f초", _label(), time.time() - start_t
        )
        return {"extracted_rules": [{"keyword": r.keyword, "fact": r.fact} for r in result.rules]}

    # -----------------------------------------------------
    # ② 하이브리드 검색
    # -----------------------------------------------------
    def retrieve_legacy_node(state: GraphState) -> Dict:
        logger.info("%s [Node 2] 하이브리드 검색 시작...", _label())
        start_t = time.time()

        if store.ensemble_retriever is None:
            logger.warning("%s ⚠️ 업로드된 문서가 없어 검색을 건너뜁니다.", _label())
            return {"search_results": []}

        search_results = []
        seen_docs = set()

        for rule in state["extracted_rules"]:
            docs = store.ensemble_retriever.invoke(rule["keyword"])
            for doc in docs:
                doc_id = doc.metadata.get("id", doc.page_content[:20])
                if doc_id in seen_docs:
                    continue
                seen_docs.add(doc_id)
                search_results.append(
                    {
                        "keyword": rule["keyword"],
                        "new_fact": rule["fact"],
                        "old_content": doc.page_content,
                        "source": doc.metadata.get("source", "Unknown"),
                        # PyPDFLoader가 채워주는 0-index 페이지 번호. DOCX 등 페이지 개념이
                        # 없는 포맷은 키 자체가 없어 None이 된다.
                        "page": doc.metadata.get("page"),
                    }
                )

        logger.info(
            "%s 👉 [Node 2 완료] 검색 소요 시간: %.3f초", _label(), time.time() - start_t
        )
        return {"search_results": search_results}

    # -----------------------------------------------------
    # ③ 충돌 검증
    # -----------------------------------------------------
    def analyze_conflicts_node(state: GraphState) -> Dict:
        logger.info("%s [Node 3] 충돌 검증 연산 시작...", _label())

        prompt = ChatPromptTemplate.from_template(
            "당신은 사내 HR 규정 검수자입니다.\n"
            "아래 '기존 콘텐츠' 내용이 '신규 규정 팩트'와 의미상 상충(불일치)하는지 판단하세요.\n\n"
            "🚨중요: 모든 분석 결과와 제안은 반드시 '한국어(Korean)'로만 작성해야 합니다. 절대 중국어나 영어를 사용하지 마세요.\n\n"
            "🚨출력 형식: 결론과 근거를 반드시 분리해서 답하세요.\n"
            "- action_suggested: 조치만 담은 한 문장 (예: '5일을 7일로 변경 권장'). 이유를 섞지 마세요.\n"
            "- reasoning: 그렇게 판단한 근거. 여기에만 설명을 자유롭게 쓰세요.\n\n"
            "기존 콘텐츠: {old_content}\n"
            "신규 규정 팩트: {new_fact}"
        )
        chain = prompt | structured_llm

        conflict_report = []
        for item in state["search_results"]:
            llm_start_t = time.time()
            analysis = chain.invoke(
                {"new_fact": item["new_fact"], "old_content": item["old_content"]}
            )
            logger.info(
                "%s 👉 [%s] 순수 LLM 추론 시간: %.2f초",
                _label(),
                item["keyword"],
                time.time() - llm_start_t,
            )

            if analysis.is_conflict:
                conflict_report.append(
                    {
                        "source": item["source"],
                        "page": item.get("page"),
                        "old_content": item["old_content"],
                        "new_fact": item["new_fact"],
                        "action_suggested": analysis.action_suggested,
                        "reasoning": analysis.reasoning,
                    }
                )

        return {"conflict_report": conflict_report}

    # -----------------------------------------------------
    # ④ 리포트 생성 (LLM 미사용)
    # -----------------------------------------------------
    def generate_report_node(state: GraphState) -> Dict:
        logger.info("%s [Node 4] 최종 리포트 생성 시작...", _label())
        start_t = time.time()

        report_data = state["conflict_report"]
        if not report_data:
            logger.info(
                "%s 👉 [Node 4 완료] 리포트 생성(충돌없음) 소요 시간: %.3f초",
                _label(),
                time.time() - start_t,
            )
            return {
                "final_markdown_report": "✅ 기존 콘텐츠 중 신규 규정과 충돌하는 항목이 발견되지 않았습니다."
            }

        markdown_lines = ["## 🚨 사내 콘텐츠 규정 충돌 검출 리포트\n"]
        markdown_lines.append("| 기존 출처(Source) | 기존 내용 (Old) | 신규 규정 (New Fact) | AI 수정 제안 |")
        markdown_lines.append("|---|---|---|---|")
        for item in report_data:
            page = item.get("page")
            source = f"{item['source']} (p.{page + 1})" if page is not None else item["source"]
            old = item["old_content"].replace("\n", " ")
            new = item["new_fact"].replace("\n", " ")
            action = item["action_suggested"].replace("\n", " ")
            markdown_lines.append(f"| {source} | {old} | {new} | **{action}** |")

        logger.info(
            "%s 👉 [Node 4 완료] 리포트 생성 소요 시간: %.3f초", _label(), time.time() - start_t
        )
        return {"final_markdown_report": "\n".join(markdown_lines)}

    # -----------------------------------------------------
    # 그래프 조립
    # -----------------------------------------------------
    workflow = StateGraph(GraphState)
    workflow.add_node("extract_rules", extract_rules_node)
    workflow.add_node("retrieve_legacy", retrieve_legacy_node)
    workflow.add_node("analyze_conflicts", analyze_conflicts_node)
    workflow.add_node("generate_report", generate_report_node)

    workflow.set_entry_point("extract_rules")
    workflow.add_edge("extract_rules", "retrieve_legacy")
    workflow.add_edge("retrieve_legacy", "analyze_conflicts")
    workflow.add_edge("analyze_conflicts", "generate_report")
    workflow.add_edge("generate_report", END)

    return workflow.compile()
