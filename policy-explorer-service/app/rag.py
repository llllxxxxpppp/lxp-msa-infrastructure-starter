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
import re
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


# 추출 텍스트에 섞이는 "의미가 0인" 문자들. seed-documents 50개를 실측해 고른 것만 지운다.
# 해설 칼럼([필수]/☞ (참고) 등)은 개정안 검토에 쓸모가 있어 지우지 않고, 아래 구분자로
# 경계만 인식시켜 규정 본문과 다른 청크로 갈라놓는다.
_NOISE_PATTERNS = [
    # 장식 구분선. 사내 문서 49개에서 45,472자(텍스트의 27%)를 차지했다.
    (re.compile(r"[\u2500-\u257f=]{5,}"), ""),
    # 페이지 번호만 있는 줄 (- 118 -).
    (re.compile(r"^\s*[-–]\s*\d+\s*[-–]\s*$", re.M), ""),
    # PDF가 중간점·쉼표 뒤에서 줄을 끊어 나열 항목이 쪼개진 것을 되붙인다.
    #   '임신‧\n출산' → '임신‧출산'   (표준취업규칙 45곳, 사내 문서 52곳)
    (re.compile(r"([‧·,、])\n(?=\S)"), r"\1"),
    # 행말 공백 → 제거, 빈 줄 3개 이상 → 2개. 이 정리로 단락 구분자 \n\n 이 생긴다.
    (re.compile(r"[ \t]+\n"), "\n"),
    (re.compile(r"\n{3,}"), "\n\n"),
]


def _normalize(text: str) -> str:
    """청킹 전에 의미 없는 문자를 걷어내고 단락 구조를 드러낸다.

    PyPDFLoader가 뽑은 원문에는 장식 구분선·페이지 번호가 섞여 있고, 빈 줄이 없어
    RecursiveCharacterTextSplitter의 1순위 구분자("\n\n")가 한 번도 매칭되지 않는다.
    이 함수를 거치면 단락 경계가 생겨 구분자가 제대로 동작한다.
    """
    for pattern, replacement in _NOISE_PATTERNS:
        text = pattern.sub(replacement, text)
    return text.strip()


def _load_document(save_path: str, ext: str) -> List[Document]:
    """확장자에 맞는 LangChain 로더로 파일을 읽고 전처리한 Document 리스트를 반환한다."""
    if ext == ".pdf":
        docs = PyPDFLoader(save_path).load()
    elif ext == ".docx":
        docs = Docx2txtLoader(save_path).load()
    else:
        raise DocumentError(f"지원하지 않는 확장자입니다: {ext}")

    # 전처리 후 알맹이가 남은 페이지만 넘긴다(표지·간지처럼 장식만 있던 페이지는 사라진다).
    normalized = []
    for doc in docs:
        doc.page_content = _normalize(doc.page_content)
        if doc.page_content:
            normalized.append(doc)
    return normalized


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

        # 기본 구분자 ["\n\n", "\n", ". ", " ", ""]는 영어 산문을 전제한 LangChain 기본값이라
        # 한국어 규정 PDF에서는 1순위(\n\n: 0회)와 3순위(". ": 문장 727개 중 160회)가
        # 사실상 작동하지 않아 결국 공백 단위로 잘렸다. 아래는 두 문서군을 모두 커버한다.
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=config.RAG_CHUNK_SIZE,
            chunk_overlap=config.RAG_CHUNK_OVERLAP,
            separators=[
                r"\n\n",                              # 단락 (_normalize가 만들어 준다)
                r"\n제\s*\d+\s*장",                    # 장 — 표준취업규칙 71회
                r"제\s*\d+\s*조\s*\(",                 # 조문 정의. 여는 괄호를 요구해
                                                       # '근로기준법 제93조' 같은 상호참조
                                                       # 369곳을 배제한다.
                r"\n(?=\s*\[[^\]\n]{2,14}\]\s*\n)",    # [ 목적 ] — 줄 전체가 대괄호인 경우만.
                                                       # 표준취업규칙의 [선택]/[필수] 인라인
                                                       # 태그 371개에서 문장이 부서지지 않게 한다.
                r"\n(?=■)",                           # ■ 섹션 — 사내 문서
                r"(?<=다\.)\s*(?=[①-⑳])",              # 항 경계
                r"(?<=다\.)",                          # 한국어 문장 끝. 룩비하인드가 없으면
                                                       # '한다.'가 '한'+'다.'로 쪼개진다.
                r"\n", r"\s", "",
            ],
            is_separator_regex=True,
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
    def _discard_stale_document(self, row) -> None:
        """실패했거나(status='failed') 처리 도중 중단된(status='uploading') 잔재 행을 정리한다.

        메타데이터 행 삭제 + 저장된 물리 파일 디렉터리 제거 + (혹시 실패가
        `vector_db.add_documents()` 이후 단계에서 났을 경우를 대비해) 그 document_id로 남아있는
        청크를 Chroma/all_chunks에서도 제거한다. 이걸 안 하면 재시도할 때마다 같은 체크섬으로
        새 행이 계속 쌓인다(재현 사례: seed-documents 자동 색인이 Ollama 콜드스타트 중 실패 →
        재기동마다 새 document_id로 재시도 → 실패 행 + 성공 행이 나란히 남음).
        """
        stale_id = row["id"]
        self.metadata_store.delete(stale_id)

        stale_dir = os.path.join(config.UPLOAD_DIR, stale_id)
        shutil.rmtree(stale_dir, ignore_errors=True)

        leftover_ids = [
            c.metadata.get("id")
            for c in self.all_chunks
            if c.metadata.get("document_id") == stale_id
        ]
        if leftover_ids:
            self.vector_db.delete(ids=leftover_ids)
            self.all_chunks = [
                c for c in self.all_chunks if c.metadata.get("document_id") != stale_id
            ]
            self._rebuild_ensemble_retriever()

    def add_document(self, filename: str, fileobj) -> Dict:
        """PDF/DOCX를 저장 -> 로드 -> 청킹 -> Chroma 적재 -> BM25 재구축.

        같은 내용(checksum)의 문서가 이미 색인 완료(status='ready')돼 있으면 재임베딩하지 않고
        기존 결과를 `status: "duplicate"`로 즉시 반환한다. API를 통한 동일 파일 재업로드 방지와
        `seed_from_directory()`가 재기동마다 시드 문서를 중복 색인하지 않는 것을 이 한 로직으로
        동시에 해결한다.

        이전 시도가 실패했거나(status='failed') 중단된(status='uploading') 잔재가 있으면, 그
        잔재를 정리한 뒤 새로 시도한다 — 그래야 재시도할 때마다 실패 행이 계속 쌓이지 않는다.
        🚨 이미 성공(ready)한 행이 있어도 **같은 체크섬의 다른 잔재 행이 남아있을 수 있으므로**
        (예: 실패 후 재시도해서 나중에 성공한 이력) 상태와 무관하게 전부 조회해서 ready가 아닌
        행은 항상 정리한다. 최근 행 1개만 보면 오래된 실패 잔재를 영영 놓친다.
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

        matches = self.metadata_store.find_all_by_checksum(checksum)
        ready_match = next((m for m in matches if m["status"] == "ready"), None)
        for stale in matches:
            if ready_match is not None and stale["id"] == ready_match["id"]:
                continue
            logger.info(
                "%s (status=%s)의 이전 시도 잔재를 정리합니다.",
                stale["original_filename"],
                stale["status"],
            )
            self._discard_stale_document(stale)

        if ready_match is not None:
            return {
                "status": "duplicate",
                "document_id": ready_match["id"],
                "filename": ready_match["original_filename"],
                "num_source_documents": 0,
                "num_chunks": ready_match["chunk_count"],
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

    def get_document(self, document_id: str) -> Optional[Dict]:
        """id로 문서 메타데이터 단건을 조회한다. 없으면 None(미리보기/다운로드용)."""
        row = self.metadata_store.get_by_id(document_id)
        return dict(row) if row else None

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
