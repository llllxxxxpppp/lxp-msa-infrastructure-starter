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
