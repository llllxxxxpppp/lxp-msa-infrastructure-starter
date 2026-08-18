"""REST API 라우터.

경로 prefix는 gateway 라우트(/api/policies/**)와 반드시 일치해야 한다.
gateway에는 StripPrefix·RewritePath 필터가 없어 경로를 그대로 전달하기 때문이다.

PoC 리포(policy-explorer-service)의 `lxp-ollama-qwen-fileupload.py` 중
"7. FastAPI 엔드포인트"를 옮긴 모듈이다. 4개 엔드포인트의 경로와 응답 형태는
그 리포 `docs/02-api-specification.md`의 계약을 그대로 유지한다.

원본과 달라진 점
  - 모듈 전역이었던 저장소/그래프를 app.state에서 꺼내 쓴다(테스트와 기동 순서 제어를 위해).
  - `response_model`을 명시해 OpenAPI 스펙에 실제 응답 스키마가 드러나게 했다.
    (PoC 리포 docs/08의 🟡 "response_model 명시" 항목)
"""

import logging
import time
from typing import Dict, List

from fastapi import APIRouter, File, HTTPException, Request, UploadFile
from pydantic import BaseModel

from app import config
from app.rag import DocumentError

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/policies", tags=["policy-explorer"])


# ---------------------------------------------------------
# 요청/응답 스키마
# ---------------------------------------------------------
class PolicyRequest(BaseModel):
    new_policy_text: str


class DocumentInfo(BaseModel):
    source: str
    chunk_count: int


class UploadResponse(BaseModel):
    status: str
    filename: str
    num_source_documents: int
    num_chunks: int
    total_chunks_in_store: int
    elapsed_seconds: float


class ResetResponse(BaseModel):
    status: str
    message: str


class AnalyzeResponse(BaseModel):
    status: str
    engine: str
    model: str
    total_time_seconds: float
    documents_in_store: int
    extracted_rules: List[Dict]
    conflict_count: int
    markdown_report: str


def _label() -> str:
    return f"[{config.CURRENT_ENGINE} | {config.CURRENT_MODEL}]"


# ---------------------------------------------------------
# 문서 관리
# ---------------------------------------------------------
@router.post("/documents/upload", response_model=UploadResponse)
async def upload_document(request: Request, file: UploadFile = File(...)):
    """PDF/DOCX를 업로드하면 청킹 후 Chroma(+BM25)에 적재해 검색 대상에 추가한다.

    🚨 Excel(.xlsx/.xls), HWP(.hwp/.hwpx)는 미지원이다.
       (사유는 PoC 리포 `select_reason.md` 8절)
    """
    store = request.app.state.store
    started = time.time()

    try:
        result = store.add_document(file.filename or "", file.file)
    except DocumentError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e
    except Exception as e:  # noqa: BLE001
        logger.error("%s ❌ 업로드 처리 중 에러: %s", _label(), e)
        raise HTTPException(status_code=500, detail=str(e)) from e

    elapsed = time.time() - started
    logger.info(
        "%s 📄 [문서 업로드 완료] %s (원본 Document %d개 → 청크 %d개, 소요 시간: %.2f초)",
        _label(),
        result["filename"],
        result["num_source_documents"],
        result["num_chunks"],
        elapsed,
    )

    return UploadResponse(status="success", elapsed_seconds=round(elapsed, 2), **result)


@router.get("/documents", response_model=List[DocumentInfo])
async def list_documents(request: Request):
    """현재 RAG 저장소에 적재된 문서(출처)별 청크 개수를 조회한다."""
    return request.app.state.store.list_documents()


@router.delete("/documents", response_model=ResetResponse)
async def reset_documents(request: Request):
    """업로드된 문서를 모두 초기화한다 (Chroma 컬렉션 + BM25 인메모리 캐시)."""
    try:
        request.app.state.store.reset()
    except Exception as e:  # noqa: BLE001
        logger.error("%s ❌ 초기화 중 에러: %s", _label(), e)
        raise HTTPException(status_code=500, detail=str(e)) from e

    return ResetResponse(status="success", message="업로드된 문서를 모두 초기화했습니다.")


# ---------------------------------------------------------
# 규정 충돌 분석
# ---------------------------------------------------------
@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_policy(request: Request, body: PolicyRequest):
    """신규 규정 텍스트를 받아 기존 문서와의 충돌을 검출하고 리포트를 반환한다."""
    store = request.app.state.store
    graph = request.app.state.graph
    api_start_t = time.time()

    if store.chunk_count == 0:
        logger.warning("%s ⚠️ 업로드된 문서가 없는 상태로 분석 요청이 들어왔습니다.", _label())

    try:
        final_state = graph.invoke({"new_policy_doc": body.new_policy_text})
    except Exception as e:  # noqa: BLE001
        logger.error("%s ❌ 에러 발생: %s", _label(), e)
        raise HTTPException(status_code=500, detail=str(e)) from e

    total_serving_time = time.time() - api_start_t
    logger.info(
        "%s ✅ [API 서빙 완료] 총 전체 처리 소요 시간: %.2f초", _label(), total_serving_time
    )

    return AnalyzeResponse(
        status="success",
        engine=config.CURRENT_ENGINE,
        model=config.CURRENT_MODEL,
        total_time_seconds=round(total_serving_time, 2),
        documents_in_store=store.chunk_count,
        extracted_rules=final_state.get("extracted_rules", []),
        conflict_count=len(final_state.get("conflict_report", [])),
        markdown_report=final_state.get("final_markdown_report", ""),
    )
