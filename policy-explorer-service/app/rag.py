"""업로드 문서 기반 RAG 저장소.

PoC 리포(policy-explorer-service)의 `lxp-ollama-qwen-fileupload.py` 중
"3. 파일 업로드 기반 RAG 저장소 설정" 및 업로드/조회/초기화 로직을 옮긴 모듈이다.

원본과 달라진 점
  1) 임베딩을 HuggingFace(`jhgan/ko-sroberta-multitask`, 768차원, 컨테이너 안 torch 계산)에서
     Ollama가 서빙하는 `bge-m3`(1024차원)로 교체했다.
  2) 원본은 모듈을 import하는 순간 Chroma에 연결하고 기존 청크를 복원했다. 그 부작용을
     없애기 위해 상태를 RagStore 클래스로 감싸고, 연결/복원 시점을 호출자가 정하게 했다.
검색 전략(Chroma 벡터 + BM25 키워드 50:50 앙상블)과 청킹 파라미터는 원본 그대로다.
근거: PoC 리포 `select_reason.md` 4~5절.
"""

import logging
import os
import shutil
from typing import Dict, List, Optional

from langchain_classic.retrievers import EnsembleRetriever
from langchain_community.document_loaders import Docx2txtLoader, PyPDFLoader
from langchain_community.retrievers import BM25Retriever
from langchain_community.vectorstores import Chroma
from langchain_core.documents import Document
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app import config

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
        """PDF/DOCX를 저장 -> 로드 -> 청킹 -> Chroma 적재 -> BM25 재구축."""
        ext = os.path.splitext(filename)[1].lower()
        if ext not in SUPPORTED_EXTENSIONS:
            supported = ", ".join(sorted(SUPPORTED_EXTENSIONS))
            raise DocumentError(
                f"지원하지 않는 파일 형식입니다 ({ext}). 업로드 가능한 확장자: {supported}"
            )

        # 🚨 TODO(PoC 리포 docs/08 🔴): 업로드 파일명을 그대로 경로에 사용하는 경로 순회
        #    취약점이 남아 있다. 원본 동작을 그대로 이식한 상태이며, 수정은 별도 작업으로
        #    분리했다(같은 파일명 재업로드 시 덮어써지는 문제도 함께 다뤄야 함).
        save_path = os.path.join(config.UPLOAD_DIR, filename)
        with open(save_path, "wb") as f:
            shutil.copyfileobj(fileobj, f)

        docs = _load_document(save_path, ext)
        if not docs:
            raise DocumentError("문서에서 텍스트를 추출하지 못했습니다.")

        chunks = self.text_splitter.split_documents(docs)
        for i, chunk in enumerate(chunks):
            chunk.metadata["source"] = filename
            chunk.metadata["id"] = f"{filename}::chunk_{i}"

        # 🚨 반드시 metadata["id"]를 Chroma 저장 id로도 넘겨야 reset()에서 삭제가 매칭된다.
        #    (넘기지 않으면 Chroma가 내부 UUID를 임의 생성해 우리 id로는 지워지지 않는다.)
        self.vector_db.add_documents(
            chunks, ids=[chunk.metadata["id"] for chunk in chunks]
        )

        self.all_chunks.extend(chunks)
        self._rebuild_ensemble_retriever()

        return {
            "filename": filename,
            "num_source_documents": len(docs),
            "num_chunks": len(chunks),
            "total_chunks_in_store": len(self.all_chunks),
        }

    def list_documents(self) -> List[Dict]:
        """적재된 문서(출처)별 청크 개수를 집계한다."""
        counts: Dict[str, int] = {}
        for chunk in self.all_chunks:
            source = chunk.metadata.get("source", "Unknown")
            counts[source] = counts.get(source, 0) + 1
        return [{"source": src, "chunk_count": cnt} for src, cnt in counts.items()]

    def reset(self) -> None:
        """Chroma 컬렉션과 BM25 인메모리 캐시를 모두 비운다."""
        ids = [c.metadata.get("id") for c in self.all_chunks if c.metadata.get("id")]
        if ids:
            self.vector_db.delete(ids=ids)
        self.all_chunks.clear()
        self.ensemble_retriever = None

    # -----------------------------------------------------
    # 상태 조회
    # -----------------------------------------------------
    @property
    def chunk_count(self) -> int:
        return len(self.all_chunks)

    def ping(self) -> int:
        """Chroma 컬렉션에 실제로 접근되는지 확인한다(헬스체크용)."""
        return self.vector_db._collection.count()
