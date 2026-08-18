/**
 * policy-explorer-service `/api/policies/analyze` 요청/응답 타입.
 * 백엔드 `app/api.py`의 `PolicyRequest`/`AnalyzeResponse`/`ConflictItem`과 1:1 대응한다.
 * (policy-explorer-service/docs/02-api-specification.md 참고)
 */
export interface AnalyzeRequest {
  new_policy_text: string;
}

export interface ExtractedRule {
  keyword: string;
  fact: string;
}

export interface ConflictItem {
  source: string;
  /** PDF의 0-index 페이지 번호. DOCX 등 페이지 개념이 없는 포맷은 null. */
  page: number | null;
  old_content: string;
  new_fact: string;
  action_suggested: string;
}

export interface AnalyzeResponse {
  status: string;
  engine: string;
  model: string;
  total_time_seconds: number;
  documents_in_store: number;
  extracted_rules: ExtractedRule[];
  conflict_count: number;
  conflicts: ConflictItem[];
  markdown_report: string;
}

/**
 * GET /api/policies/documents 응답 항목. 백엔드 `app/api.py`의 `DocumentInfo`와 1:1 대응
 * (문서 메타데이터 SQLite DB 기준 — app/metadata_db.py).
 */
export interface DocumentInfo {
  id: string;
  original_filename: string;
  /** "uploading" | "ready" | "failed" */
  status: string;
  chunk_count: number;
  size_bytes: number;
  uploaded_at: string;
  error_message: string | null;
}

/** POST /api/policies/documents/upload 응답. 백엔드 `UploadResponse`와 1:1 대응. */
export interface UploadResponse {
  /** "success" | "duplicate"(동일 내용 문서가 이미 색인돼 있어 재임베딩을 건너뛴 경우) */
  status: string;
  document_id: string;
  filename: string;
  num_source_documents: number;
  num_chunks: number;
  total_chunks_in_store: number;
  elapsed_seconds: number;
}
