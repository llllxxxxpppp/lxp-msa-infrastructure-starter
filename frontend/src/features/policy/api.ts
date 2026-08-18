import { apiFetch } from "@/lib/api-client";
import type { AnalyzeRequest, AnalyzeResponse } from "./types";

/**
 * POST /api/policies/analyze — policy-explorer-service `analyze_policy`.
 * 신규 규정 텍스트를 넣으면 LangGraph 파이프라인(추출→검색→충돌분석→리포트)을 실행해
 * 충돌 항목(`conflicts`)과 마크다운 리포트를 반환한다. ROLE_ADMIN 필요(gateway에서 인가).
 */
export function analyzePolicy(body: AnalyzeRequest): Promise<AnalyzeResponse> {
  return apiFetch<AnalyzeResponse>("/api/policies/analyze", { method: "POST", body });
}
