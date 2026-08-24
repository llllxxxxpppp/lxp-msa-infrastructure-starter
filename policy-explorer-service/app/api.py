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
import mimetypes
import os
import time
from typing import Dict, List, Optional

from fastapi import APIRouter, File, HTTPException, Request, UploadFile
from fastapi.responses import FileResponse
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
    id: str
    original_filename: str
    status: str
    chunk_count: int
    size_bytes: int
    uploaded_at: str
    error_message: Optional[str] = None


class UploadResponse(BaseModel):
    # "success" | "duplicate"(동일 내용 문서가 이미 색인돼 있어 재임베딩을 건너뛴 경우)
    status: str
    document_id: str
    filename: str
    num_source_documents: int
    num_chunks: int
    total_chunks_in_store: int
    elapsed_seconds: float


class ResetResponse(BaseModel):
    status: str
    message: str


class ConflictItem(BaseModel):
    source: str
    # PyPDFLoader가 채운 0-index 페이지 번호. DOCX 등 페이지 개념이 없으면 None.
    page: Optional[int] = None
    old_content: str
    new_fact: str
    action_suggested: str
    # 판단 근거. action_suggested(한 문장 조치)와 분리해 UI가 결론/근거를 따로 렌더링할 수
    # 있게 한다 — 한 필드에 섞어두면 긴 서술형 문단으로 뭉쳐 가독성이 떨어졌다.
    reasoning: str


class AnalyzeResponse(BaseModel):
    status: str
    engine: str
    model: str
    total_time_seconds: float
    documents_in_store: int
    extracted_rules: List[Dict]
    conflict_count: int
    # 🆕 파일업로드 버전 전용 additive 확장(예: documents_in_store와 동일한 취지).
    #    "해당 파일 및 위치"/"변경 제안 상세" UI가 markdown_report를 파싱하지 않고도
    #    구조화된 데이터로 바로 렌더링할 수 있도록 conflict_report를 그대로 노출한다.
    conflicts: List[ConflictItem] = []
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
        "%s 📄 [문서 업로드 %s] %s (원본 Document %d개 → 청크 %d개, 소요 시간: %.2f초)",
        _label(),
        result["status"],
        result["filename"],
        result["num_source_documents"],
        result["num_chunks"],
        elapsed,
    )

    # result에 이미 status/document_id/filename 등이 들어있다(app/rag.py RagStore.add_document).
    return UploadResponse(elapsed_seconds=round(elapsed, 2), **result)


@router.get("/documents", response_model=List[DocumentInfo])
async def list_documents(request: Request):
    """현재 RAG 저장소에 적재된 문서(출처)별 청크 개수를 조회한다."""
    return request.app.state.store.list_documents()


@router.get("/documents/{document_id}/content")
async def get_document_content(document_id: str, request: Request):
    """개별 문서의 원본 파일을 그대로 스트리밍한다(백오피스 미리보기/다운로드용)."""
    store = request.app.state.store
    doc = store.get_document(document_id)
    if doc is None:
        raise HTTPException(status_code=404, detail="문서를 찾을 수 없습니다.")

    file_path = os.path.join(config.UPLOAD_DIR, doc["storage_key"])
    if not os.path.isfile(file_path):
        raise HTTPException(status_code=404, detail="파일이 존재하지 않습니다.")

    # content_type 컬럼은 확장자 문자열(예: ".pdf")이 저장돼 있어(RagStore.add_document) MIME
    # 타입으로 그대로 쓸 수 없다. 파일명 기준으로 실제 MIME 타입을 다시 계산한다.
    media_type = mimetypes.guess_type(doc["original_filename"])[0] or "application/octet-stream"
    return FileResponse(
        file_path,
        media_type=media_type,
        filename=doc["original_filename"],
        content_disposition_type="inline",
    )


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

    conflict_report = final_state.get("conflict_report", [])

    return AnalyzeResponse(
        status="success",
        engine=config.CURRENT_ENGINE,
        model=config.CURRENT_MODEL,
        total_time_seconds=round(total_serving_time, 2),
        documents_in_store=store.chunk_count,
        extracted_rules=final_state.get("extracted_rules", []),
        conflict_count=len(conflict_report),
        conflicts=[ConflictItem(**item) for item in conflict_report],
        markdown_report=final_state.get("final_markdown_report", ""),
    )
