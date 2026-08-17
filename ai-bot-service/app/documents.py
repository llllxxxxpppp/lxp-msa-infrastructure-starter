import os
from io import BytesIO
from uuid import uuid4

from fastapi import APIRouter, File, HTTPException, Response, UploadFile
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader
from pypdf.errors import PdfReadError

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


@router.post("", status_code=201)
def upload_document(
    course_id: int,
    file: UploadFile = File(...),
) -> dict:
    """PDF를 처리하여 Chroma에 저장한다."""

    filename = file.filename or ""

    # PDF 확장자만 허용
    if not filename.lower().endswith(".pdf"):
        raise HTTPException(
            status_code=400,
            detail="PDF 파일만 업로드할 수 있습니다.",
        )

    try:
        # 업로드된 PDF를 메모리에서 바로 읽는다.
        reader = PdfReader(BytesIO(file.file.read()))
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

    except PdfReadError as exc:
        raise HTTPException(
            status_code=400,
            detail="PDF 파일을 읽을 수 없습니다.",
        ) from exc

    # 스캔 PDF처럼 텍스트를 추출할 수 없는 경우
    if not documents:
        raise HTTPException(
            status_code=400,
            detail="PDF에서 텍스트를 찾을 수 없습니다.",
        )

    # 업로드된 PDF 하나를 구분하는 고유 ID
    document_id = str(uuid4())

    # 각 청크에 문서 ID와 순번을 저장한다.
    for index, document in enumerate(documents):
        document.metadata["document_id"] = document_id
        document.metadata["chunk_index"] = index

    # 각 청크를 임베딩한 후 Chroma에 저장한다.
    vector_store.add_documents(
        documents=documents,
        ids=[
            f"{document_id}:{index}"
            for index in range(len(documents))
        ],
    )

    return {
        "document_id": document_id,
        "filename": filename,
        "chunk_count": len(documents),
    }


@router.get("")
def list_documents(course_id: int) -> list[dict]:
    """강좌에 등록된 PDF 목록을 조회한다."""

    # course_id가 같은 청크만 조회한다.
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
) -> Response:
    """PDF에 해당하는 모든 청크를 삭제한다."""

    # 지정된 강좌와 문서에 속한 청크 ID를 조회한다.
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

    # PDF에 속한 모든 청크를 Chroma에서 삭제한다.
    vector_store.delete(ids=chunk_ids)

    return Response(status_code=204)