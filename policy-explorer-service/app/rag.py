"""업로드 문서 기반 RAG 저장소.

PoC 리포(policy-explorer-service)의 `lxp-ollama-qwen-fileupload.py` 중
"3. 파일 업로드 기반 RAG 저장소 설정" 및 업로드/조회/초기화 로직을 옮긴 모듈이다.

원본과 달라진 점
  1) 임베딩을 HuggingFace(`jhgan/ko-sroberta-multitask`, 768차원, 컨테이너 안 torch 계산)에서
     Ollama가 서빙하는 `bge-m3`(1024차원)로 교체했다.
  2) 원본은 모듈을 import하는 순간 Chroma에 연결하고 기존 청크를 복원했다. 그 부작용을
     없애기 위해 상태를 RagStore 클래스로 감싸고, 연결/복원 시점을 호출자가 정하게 했다.
  3) Chroma를 폐기 예정인 `langchain_community.vectorstores`에서 전용 독립 패키지
     `langchain_chroma`로 교체했다(같은 리포의 ai-bot 선례와도 일치). 다만 아래 로더와
     BM25Retriever는 독립 패키지가 없어 langchain-community 의존이 남는다.
검색 전략(Chroma 벡터 + BM25 키워드 50:50 앙상블)과 청킹 파라미터는 원본 그대로다.
근거: PoC 리포 `select_reason.md` 4~5절.
"""

import glob
import hashlib
import logging
import os
import shutil
import uuid
from typing import Dict, List, Optional

from langchain_classic.retrievers import EnsembleRetriever
from langchain_chroma import Chroma
from langchain_community.document_loaders import Docx2txtLoader, PyPDFLoader
from langchain_community.retrievers import BM25Retriever
from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app import config
from app.metadata_db import DocumentMetadataStore

logger = logging.getLogger(__name__)

# 지원 확장자. 새 포맷을 추가할 때는 이 집합과 _load_document에만 등록하면 된다.
# (Excel/HWP 제외 사유는 PoC 리포 `select_reason.md` 8절 참고)
SUPPORTED_EXTENSIONS = {".pdf", ".docx"}


class DocumentError(Exception):
    """업로드 요청 자체가 잘못된 경우(확장자 미지원, 텍스트 추출 실패 등)."""


def _load_document(save_path: str, ext: str) -> List[Document]:
    """확장자에 맞는 LangChain 로더로 파일을 읽어 Document 리스트를 반환한다."""
    if ext == ".pdf":
        return PyPDFLoader(save_path).load()
    if ext == ".docx":
        return Docx2txtLoader(save_path).load()
    raise DocumentError(f"지원하지 않는 확장자입니다: {ext}")


class RagStore:
    """Chroma(벡터) + BM25(키워드) 하이브리드 검색 저장소.

    BM25Retriever는 증분 추가 API가 없는 인메모리 구조라, 업로드된 전체 청크를
    `all_chunks`에 누적해두고 업로드마다 재구축한다(원본과 동일한 전략).
    이 때문에 이 서비스는 단일 인스턴스 전제다 — PoC 리포 `docs/07` 참고.
    """

    def __init__(self) -> None:
        # 임베딩과 생성 모델이 모두 같은 Ollama 서버를 바라본다.
        self.embeddings = OllamaEmbeddings(
            model=config.OLLAMA_EMBEDDING_MODEL,
            base_url=config.OLLAMA_BASE_URL,
        )

        self.vector_db = Chroma(
            collection_name=config.CHROMA_COLLECTION_NAME,
            embedding_function=self.embeddings,
            persist_directory=config.CHROMA_PERSIST_DIR,
        )
        self.chroma_retriever = self.vector_db.as_retriever(
            search_kwargs={"k": config.RAG_TOP_K}
        )

        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=config.RAG_CHUNK_SIZE,
            chunk_overlap=config.RAG_CHUNK_OVERLAP,
            separators=["\n\n", "\n", ". ", " ", ""],
        )

        self.all_chunks: List[Document] = []
        self.ensemble_retriever: Optional[EnsembleRetriever] = None

        os.makedirs(config.UPLOAD_DIR, exist_ok=True)

        self.metadata_store = DocumentMetadataStore(config.METADATA_DB_PATH)

    # -----------------------------------------------------
    # 리트리버 관리
    # -----------------------------------------------------
    def _rebuild_ensemble_retriever(self) -> None:
        """누적된 전체 청크로 BM25를 재구축하고 앙상블 리트리버를 갱신한다."""
        if not self.all_chunks:
            self.ensemble_retriever = None
            return

        bm25_retriever = BM25Retriever.from_documents(self.all_chunks)
        bm25_retriever.k = config.RAG_TOP_K

        # 벡터 50% + 키워드 50%
        self.ensemble_retriever = EnsembleRetriever(
            retrievers=[self.chroma_retriever, bm25_retriever],
            weights=[0.5, 0.5],
        )

    def restore_from_persisted(self) -> int:
        """재기동 시 Chroma에 남아 있는 청크를 불러와 BM25/앙상블을 복원한다."""
        try:
            existing = self.vector_db.get(include=["documents", "metadatas"])
        except Exception as e:  # noqa: BLE001 - 복원 실패는 기동을 막지 않는다
            logger.warning("기존 벡터 컬렉션을 불러오지 못했습니다: %s", e)
            return 0

        contents = existing.get("documents") or []
        metadatas = existing.get("metadatas") or []
        if not contents:
            return 0

        for content, metadata in zip(contents, metadatas):
            self.all_chunks.append(
                Document(page_content=content, metadata=metadata or {})
            )

        self._rebuild_ensemble_retriever()
        logger.info("[Startup] 기존 업로드 문서 %d개 청크를 복원했습니다.", len(self.all_chunks))
        return len(self.all_chunks)

    # -----------------------------------------------------
    # 업로드 / 조회 / 초기화
    # -----------------------------------------------------
    def add_document(self, filename: str, fileobj) -> Dict:
        """PDF/DOCX를 저장 -> 로드 -> 청킹 -> Chroma 적재 -> BM25 재구축.

        같은 내용(checksum)의 문서가 이미 색인돼 있으면 재임베딩하지 않고 기존 결과를
        `status: "duplicate"`로 즉시 반환한다. API를 통한 동일 파일 재업로드 방지와
        `seed_from_directory()`가 재기동마다 시드 문서를 중복 색인하지 않는 것을 이 한
        로직으로 동시에 해결한다.
        """
        original_filename = os.path.basename(filename)
        ext = os.path.splitext(original_filename)[1].lower()
        if ext not in SUPPORTED_EXTENSIONS:
            supported = ", ".join(sorted(SUPPORTED_EXTENSIONS))
            raise DocumentError(
                f"지원하지 않는 파일 형식입니다 ({ext}). 업로드 가능한 확장자: {supported}"
            )

        data = fileobj.read()
        checksum = hashlib.sha256(data).hexdigest()

        existing = self.metadata_store.find_ready_by_checksum(checksum)
        if existing is not None:
            return {
                "status": "duplicate",
                "document_id": existing["id"],
                "filename": existing["original_filename"],
                "num_source_documents": 0,
                "num_chunks": existing["chunk_count"],
                "total_chunks_in_store": len(self.all_chunks),
            }

        # 🔒 파일명을 저장 키로 직접 쓰지 않는다. document_id(UUID) 하위 디렉터리에 저장해
        #    경로 순회 취약점과 동일 파일명 재업로드 덮어쓰기 문제를 함께 없앤다.
        #    (PoC 리포 docs/08 🔴 항목 — docs/09 메타데이터 DB 도입과 함께 처리)
        doc_id = uuid.uuid4().hex
        storage_key = f"{doc_id}/{original_filename}"
        save_path = os.path.join(config.UPLOAD_DIR, storage_key)
        os.makedirs(os.path.dirname(save_path), exist_ok=True)

        self.metadata_store.create(
            id=doc_id,
            original_filename=original_filename,
            storage_key=storage_key,
            content_type=ext,
            size_bytes=len(data),
            checksum_sha256=checksum,
        )

        try:
            with open(save_path, "wb") as f:
                f.write(data)

            docs = _load_document(save_path, ext)
            if not docs:
                raise DocumentError("문서에서 텍스트를 추출하지 못했습니다.")

            chunks = self.text_splitter.split_documents(docs)
            for i, chunk in enumerate(chunks):
                chunk.metadata["source"] = original_filename
                chunk.metadata["document_id"] = doc_id
                chunk.metadata["id"] = f"{doc_id}::chunk_{i}"

            # 🚨 반드시 metadata["id"]를 Chroma 저장 id로도 넘겨야 reset()에서 삭제가 매칭된다.
            #    (넘기지 않으면 Chroma가 내부 UUID를 임의 생성해 우리 id로는 지워지지 않는다.)
            self.vector_db.add_documents(
                chunks, ids=[chunk.metadata["id"] for chunk in chunks]
            )

            self.all_chunks.extend(chunks)
            self._rebuild_ensemble_retriever()
        except Exception as e:  # noqa: BLE001 - 실패 사유를 메타데이터에 남기고 그대로 재발생시킨다
            self.metadata_store.mark_failed(doc_id, error_message=str(e))
            raise

        self.metadata_store.mark_ready(doc_id, chunk_count=len(chunks))

        return {
            "status": "success",
            "document_id": doc_id,
            "filename": original_filename,
            "num_source_documents": len(docs),
            "num_chunks": len(chunks),
            "total_chunks_in_store": len(self.all_chunks),
        }

    def list_documents(self) -> List[Dict]:
        """문서 단위 메타데이터(SQLite)를 그대로 반환한다.

        예전에는 `all_chunks`를 매번 순회해 파일명별 청크 수를 역산했다(docs/09가 지적한
        문제). 이제 문서가 SQLite에 1급 개념으로 존재하므로 그대로 조회한다.
        """
        return [dict(row) for row in self.metadata_store.list_active()]

    def reset(self) -> None:
        """Chroma 컬렉션 + BM25 인메모리 캐시 + 메타데이터 DB + 업로드 원본 파일을 모두 비운다."""
        ids = [c.metadata.get("id") for c in self.all_chunks if c.metadata.get("id")]
        if ids:
            self.vector_db.delete(ids=ids)
        self.all_chunks.clear()
        self.ensemble_retriever = None
        self.metadata_store.delete_all()

        # document_id 하위 디렉터리째로 남는 고아 파일을 방지한다.
        shutil.rmtree(config.UPLOAD_DIR, ignore_errors=True)
        os.makedirs(config.UPLOAD_DIR, exist_ok=True)

    def seed_from_directory(self, dir_path: str) -> Dict[str, int]:
        """컨테이너 기동 시 자동 색인. `add_document()`를 그대로 재사용해 로직을 중복하지 않는다.

        디렉터리가 없으면 조용히 0건 처리한다 — 샘플 문서 없이 띄우는 로컬 개발 환경도
        정상 기동해야 한다. 이미 색인된(checksum 일치) 파일은 `add_document()`의 중복 검사
        로직 덕분에 재임베딩되지 않는다.
        """
        result = {"ingested": 0, "duplicate": 0, "failed": 0}
        if not os.path.isdir(dir_path):
            return result

        patterns = [f"*{ext}" for ext in sorted(SUPPORTED_EXTENSIONS)]
        paths = sorted(
            p for pattern in patterns for p in glob.glob(os.path.join(dir_path, pattern))
        )
        for path in paths:
            filename = os.path.basename(path)
            try:
                with open(path, "rb") as f:
                    outcome = self.add_document(filename, f)
                key = "duplicate" if outcome.get("status") == "duplicate" else "ingested"
                result[key] += 1
            except Exception as e:  # noqa: BLE001 - 시드 문서 하나의 실패가 기동을 막으면 안 된다
                logger.warning("시드 문서 색인 실패: %s (%s)", filename, e)
                result["failed"] += 1
        return result

    # -----------------------------------------------------
    # 상태 조회
    # -----------------------------------------------------
    @property
    def chunk_count(self) -> int:
        return len(self.all_chunks)

    def ping(self) -> int:
        """Chroma 컬렉션에 실제로 접근되는지 확인한다(헬스체크용).

        라이브러리 내부 구현(`_collection`)에 기대지 않도록 공개 API만 사용한다.
        컬렉션 전체 id를 훑으므로 O(n)이지만, 사내 문서 규모에서는 문제되지 않는다.
        """
        return len(self.vector_db.get(include=[])["ids"])
