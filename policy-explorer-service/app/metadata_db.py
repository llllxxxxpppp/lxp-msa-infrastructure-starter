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

    def find_ready_by_checksum(self, checksum_sha256: str) -> Optional[sqlite3.Row]:
        """이미 같은 내용(checksum)으로 색인 완료된 문서가 있으면 그 행을 반환한다."""
        with self._connect() as conn:
            cursor = conn.execute(
                "SELECT * FROM documents WHERE checksum_sha256=? AND status='ready' AND deleted_at IS NULL"
                " ORDER BY uploaded_at LIMIT 1",
                (checksum_sha256,),
            )
            return cursor.fetchone()

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
