"""문서 단위 메타데이터 SQLite 저장소.

PoC 리포에는 없던 신규 계층이다. `docs/09-data-architecture.md`가 지적한 공백
(문서 단위 메타데이터 부재, 동일 파일명 재업로드 시 조용한 덮어쓰기)을 메우기 위해
이번에 실제로 구현했다. 스키마는 그 문서가 제안한 것을 그대로 따른다.

이 서비스는 BM25 인메모리 구조 때문에 이미 단일 인스턴스 전제다(`docs/07`). SQLite의
단일 라이터 제약이 새로운 제약을 추가하지 않는다. 커넥션은 호출마다 짧게 열고 닫아
(`sqlite3.connect()` per call) 스레드 안전성 문제를 피하고, WAL 모드로 읽기/쓰기 동시성을
개선한다.
"""

import os
import sqlite3
from contextlib import contextmanager
from typing import Iterator, Optional

SCHEMA = """
CREATE TABLE IF NOT EXISTS documents (
    id                 TEXT PRIMARY KEY,
    original_filename  TEXT NOT NULL,
    storage_key        TEXT NOT NULL UNIQUE,
    content_type       TEXT,
    size_bytes         INTEGER,
    checksum_sha256    TEXT,
    status             TEXT NOT NULL,
    chunk_count        INTEGER DEFAULT 0,
    error_message      TEXT,
    uploaded_by        TEXT,
    uploaded_at        TEXT NOT NULL,
    updated_at         TEXT NOT NULL,
    deleted_at         TEXT
);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_checksum ON documents(checksum_sha256);
"""


def _now_iso() -> str:
    # config.py와 마찬가지로 이 모듈도 외부 상태(시계 포함)를 최대한 얇게 다룬다.
    # datetime.utcnow()는 3.12부터 deprecated라 timezone-aware로 생성한다.
    from datetime import datetime, timezone

    return datetime.now(timezone.utc).isoformat()


class DocumentMetadataStore:
    """`documents` 테이블에 대한 CRUD. 커넥션 풀 없이 호출마다 새로 연다."""

    def __init__(self, db_path: str) -> None:
        self.db_path = db_path
        parent = os.path.dirname(db_path)
        if parent:
            os.makedirs(parent, exist_ok=True)
        with self._connect() as conn:
            conn.executescript(SCHEMA)

    @contextmanager
    def _connect(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(self.db_path)
        try:
            conn.execute("PRAGMA journal_mode=WAL;")
            conn.row_factory = sqlite3.Row
            yield conn
            conn.commit()
        finally:
            conn.close()

    def create(
        self,
        *,
        id: str,
        original_filename: str,
        storage_key: str,
        content_type: Optional[str],
        size_bytes: int,
        checksum_sha256: str,
    ) -> None:
        now = _now_iso()
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO documents (
                    id, original_filename, storage_key, content_type, size_bytes,
                    checksum_sha256, status, chunk_count, uploaded_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'uploading', 0, ?, ?)
                """,
                (id, original_filename, storage_key, content_type, size_bytes, checksum_sha256, now, now),
            )

    def mark_ready(self, id: str, *, chunk_count: int) -> None:
        with self._connect() as conn:
            conn.execute(
                "UPDATE documents SET status='ready', chunk_count=?, updated_at=? WHERE id=?",
                (chunk_count, _now_iso(), id),
            )

    def mark_failed(self, id: str, *, error_message: str) -> None:
        with self._connect() as conn:
            conn.execute(
                "UPDATE documents SET status='failed', error_message=?, updated_at=? WHERE id=?",
                (error_message, _now_iso(), id),
            )

    def get_by_id(self, id: str) -> Optional[sqlite3.Row]:
        with self._connect() as conn:
            cursor = conn.execute(
                "SELECT * FROM documents WHERE id=? AND deleted_at IS NULL", (id,)
            )
            return cursor.fetchone()

    def find_all_by_checksum(self, checksum_sha256: str) -> list[sqlite3.Row]:
        """상태와 무관하게 체크섬이 일치하는 모든 행을 반환한다(오래된 실패/중단 잔재까지 전부).

        단일 행(가장 최근 것)만 보면, "실패 후 재시도해서 나중에 성공한" 이력이 있는 문서의
        경우 성공(ready) 행이 더 최근이라 그것만 걸리고 더 오래된 failed 잔재는 영원히
        발견되지 않는다(실제 재현된 버그). 호출부가 ready 여부와 무관하게 이 목록 전체를 훑어
        ready가 아닌 행을 전부 정리해야, 재기동/재업로드를 몇 번 반복해도 체크섬당 행이
        1개로 수렴한다.
        """
        with self._connect() as conn:
            cursor = conn.execute(
                "SELECT * FROM documents WHERE checksum_sha256=? AND deleted_at IS NULL"
                " ORDER BY uploaded_at",
                (checksum_sha256,),
            )
            return cursor.fetchall()

    def delete(self, id: str) -> None:
        """단일 행을 하드 삭제한다(전체 초기화용 `delete_all()`과 별개).

        실패/중단된 시도의 잔재 행을 지우고 재시도할 때 쓴다.
        """
        with self._connect() as conn:
            conn.execute("DELETE FROM documents WHERE id=?", (id,))

    def list_active(self) -> list[sqlite3.Row]:
        with self._connect() as conn:
            cursor = conn.execute(
                "SELECT * FROM documents WHERE deleted_at IS NULL ORDER BY uploaded_at"
            )
            return cursor.fetchall()

    def delete_all(self) -> None:
        """전체 초기화(`DELETE /api/policies/documents`) 전용 — soft delete가 아니라 완전 삭제."""
        with self._connect() as conn:
            conn.execute("DELETE FROM documents")
