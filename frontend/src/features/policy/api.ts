import { apiFetch, apiFetchResponse } from "@/lib/api-client";
import type { AnalyzeRequest, AnalyzeResponse, DocumentInfo, UploadResponse } from "./types";

/**
 * POST /api/policies/analyze — policy-explorer-service `analyze_policy`.
 * 신규 규정 텍스트를 넣으면 LangGraph 파이프라인(추출→검색→충돌분석→리포트)을 실행해
 * 충돌 항목(`conflicts`)과 마크다운 리포트를 반환한다. ROLE_ADMIN 필요(gateway에서 인가).
 */
export function analyzePolicy(body: AnalyzeRequest): Promise<AnalyzeResponse> {
  return apiFetch<AnalyzeResponse>("/api/policies/analyze", { method: "POST", body });
}

/** GET /api/policies/documents — 문서 메타데이터 DB(SQLite) 기준 문서 목록. */
export function listDocuments(): Promise<DocumentInfo[]> {
  return apiFetch<DocumentInfo[]>("/api/policies/documents");
}

/**
 * POST /api/policies/documents/upload — PDF/DOCX 업로드(multipart/form-data).
 * `apiFetch`가 `body instanceof FormData`를 감지해 Content-Type을 브라우저에 맡긴다.
 */
export function uploadDocument(file: File): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append("file", file);
  return apiFetch<UploadResponse>("/api/policies/documents/upload", {
    method: "POST",
    body: formData,
  });
}

/**
 * GET /api/policies/documents/{id}/content — 원본 파일을 Blob으로 가져온다(미리보기/다운로드용).
 * `<iframe src="...">`에 게이트웨이 절대 URL을 직접 넣으면 Authorization 헤더가 안 붙어 401이
 * 나므로, `apiFetchResponse`로 인증 헤더를 붙여 받은 뒤 호출부에서 `URL.createObjectURL`로
 * object URL을 만들어 써야 한다.
 */
export async function getDocumentContent(id: string): Promise<Blob> {
  const res = await apiFetchResponse(`/api/policies/documents/${id}/content`);
  return res.blob();
}
