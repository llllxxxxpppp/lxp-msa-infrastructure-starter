import os

# [추가]
import json
import logging
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen
from io import BytesIO
from uuid import uuid4

# 기존 FastAPI import에 Header 추가
from fastapi import APIRouter, File, Header, HTTPException, Response, UploadFile
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader
from pypdf.errors import PdfReadError

logger = logging.getLogger("uvicorn.error")

router = APIRouter(
    prefix="/api/ai/courses/{course_id}/documents",
    tags=["documents"],
)

# Ollama에서 실행 중인 로컬 임베딩 모델
embeddings = OllamaEmbeddings(
    model=os.getenv(
        "OLLAMA_EMBEDDING_MODEL",
        "qwen3-embedding:0.6b",
    ),
    base_url=os.getenv(
        "OLLAMA_BASE_URL",
        "http://localhost:11434",
    ),
)

# 벡터와 원문, 메타데이터를 로컬 Chroma에 저장
vector_store = Chroma(
    collection_name="course_documents",
    embedding_function=embeddings,
    persist_directory="data/chroma",
)

# 페이지 텍스트를 1,000자 단위로 분할
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=150,
)


# [추가] 현재 강사가 해당 강좌의 담당자인지 확인한다.
def verify_course_owner(course_id: int, user_id: int) -> None:
    course_service_url = os.getenv(
        "COURSE_SERVICE_URL",
        "http://localhost:8083",
    ).rstrip("/")

    url = f"{course_service_url}/internal/courses/by-instructor/{user_id}"

    try:
        with urlopen(url, timeout=5) as response:
            courses = json.load(response)
    except (URLError, TimeoutError, json.JSONDecodeError) as exc:
        raise HTTPException(
            status_code=503,
            detail="강좌 정보를 확인할 수 없습니다.",
        ) from exc

    is_owner = any(course.get("courseId") == course_id for course in courses)

    if not is_owner:
        raise HTTPException(
            status_code=403,
            detail="담당 강좌에만 PDF를 관리할 수 있습니다.",
        )


# [추가] PDF를 파싱해서 청크로 분할하고 Chroma에 저장한다. 업로드 API와 demo 자동 등록이 공통으로 사용한다.
def index_pdf(course_id: int, filename: str, file_bytes: bytes) -> tuple[str | None, int]:
    # PDF를 메모리에서 바로 읽는다.
    reader = PdfReader(BytesIO(file_bytes))
    documents = []

    # 페이지별로 텍스트를 추출한다.
    for page_number, page in enumerate(reader.pages, start=1):
        text = (page.extract_text() or "").strip()

        # 텍스트가 없는 페이지는 제외한다.
        if not text:
            continue

        # 페이지 텍스트를 여러 청크로 분할한다.
        chunks = text_splitter.create_documents(
            [text],
            metadatas=[
                {
                    "course_id": course_id,
                    "filename": filename,
                    "page_number": page_number,
                }
            ],
        )

        documents.extend(chunks)

    # 스캔 PDF처럼 텍스트를 추출할 수 없는 경우
    if not documents:
        return None, 0

    # 업로드된 PDF 하나를 구분하는 고유 ID
    document_id = str(uuid4())

    # 각 청크에 문서 ID와 순번을 저장한다.
    for index, document in enumerate(documents):
        document.metadata["document_id"] = document_id
        document.metadata["chunk_index"] = index

    # 각 청크를 임베딩한 후 Chroma에 저장한다.
    vector_store.add_documents(
        documents=documents,
        ids=[f"{document_id}:{index}" for index in range(len(documents))],
    )

    return document_id, len(documents)


# [추가] course_id + filename 기준으로 이미 등록된 PDF인지 확인한다.
def is_pdf_indexed(course_id: int, filename: str) -> bool:
    result = vector_store.get(
        where={
            "$and": [
                {"course_id": {"$eq": course_id}},
                {"filename": {"$eq": filename}},
            ]
        }
    )

    return bool(result.get("ids"))


# [추가] 서비스 시작 시 demo 폴더의 PDF를 발표용 강좌에 자동으로 등록한다.
def index_demo_pdfs() -> None:
    course_id_raw = os.getenv("DEMO_COURSE_ID")

    if not course_id_raw:
        return

    course_id = int(course_id_raw)
    demo_dir = Path(__file__).resolve().parent.parent / "demo"

    if not demo_dir.is_dir():
        return

    for pdf_path in sorted(demo_dir.glob("*.pdf")):
        filename = pdf_path.name

        if is_pdf_indexed(course_id, filename):
            logger.info("Demo PDF already exists, skip: %s", filename)
            continue

        try:
            document_id, _ = index_pdf(course_id, filename, pdf_path.read_bytes())
        except Exception:
            logger.warning("Demo PDF failed to index, skip: %s", filename, exc_info=True)
            continue

        if document_id is None:
            logger.warning("Demo PDF has no extractable text, skip: %s", filename)
            continue

        logger.info("Demo PDF indexed: %s", filename)


@router.post("", status_code=201)
async def upload_document(
    course_id: int,
    file: UploadFile = File(...),
    user_id: int = Header(alias="X-User-Id"),
):
    """PDF를 처리하여 Chroma에 저장한다."""

    # 담당 강좌인지 확인한 후 PDF를 처리한다.
    verify_course_owner(course_id, user_id)

    filename = file.filename or ""

    # PDF 확장자만 허용
    if not filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail="PDF 파일만 업로드할 수 있습니다.",
        )

    try:
        document_id, chunk_count = index_pdf(course_id, filename, file.file.read())
    except PdfReadError as exc:
        raise HTTPException(
            status_code=400,
            detail="PDF 파일을 읽을 수 없습니다.",
        ) from exc

    # 스캔 PDF처럼 텍스트를 추출할 수 없는 경우
    if document_id is None:
        raise HTTPException(
            status_code=400,
            detail="PDF에서 텍스트를 찾을 수 없습니다.",
        )

    return {
        "document_id": document_id,
        "filename": filename,
        "chunk_count": chunk_count,
    }


@router.get("")
def list_documents(
    course_id: int,
    user_id: int = Header(alias="X-User-Id"),
):
    """강좌에 등록된 PDF 목록을 조회한다."""

    # 담당 강좌인지 확인한 후 목록을 조회한다.
    verify_course_owner(course_id, user_id)

    result = vector_store.get(
        where={"course_id": course_id},
        include=["metadatas"],
    )

    documents = {}

    # 하나의 PDF가 여러 청크로 저장되므로 document_id로 중복 제거
    for metadata in result.get("metadatas") or []:
        document_id = metadata["document_id"]

        documents[document_id] = {
            "document_id": document_id,
            "filename": metadata["filename"],
        }

    return list(documents.values())


@router.delete("/{document_id}", status_code=204)
def delete_document(
    course_id: int,
    document_id: str,
    user_id: int = Header(alias="X-User-Id"),
):
    """PDF에 해당하는 모든 청크를 삭제한다."""

    # 담당 강좌인지 확인한 후 문서를 삭제한다.
    verify_course_owner(course_id, user_id)

    result = vector_store.get(
        where={
            "$and": [
                {"course_id": {"$eq": course_id}},
                {"document_id": {"$eq": document_id}},
            ]
        }
    )

    chunk_ids = result.get("ids") or []

    if not chunk_ids:
        raise HTTPException(
            status_code=404,
            detail="강의 자료를 찾을 수 없습니다.",
        )

    vector_store.delete(ids=chunk_ids)

    return Response(status_code=204)