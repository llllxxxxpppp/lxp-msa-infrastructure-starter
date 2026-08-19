import { apiFetch } from "@/lib/api-client";
import type { ChatClient, ChatRequest, ChatResponse } from "./types";

const CHAT_PATH = "/api/curriculum/chat";
const REVIEW_PROMPT =
  "이 커리큘럼이 괜찮으신가요? 바꾸고 싶은 부분이 있다면 말씀해 주세요.";

/**
 * reviewing 응답의 message에는 카드와 동일한 커리큘럼이 텍스트로도 들어 있다.
 * 화면은 curriculum 필드로 카드를 그리므로 마지막 검토 안내 문장만 남긴다.
 */
function normalizeResponse(response: ChatResponse): ChatResponse {
  if (response.status !== "reviewing" || !response.curriculum) {
    return response;
  }

  const lines = response.message
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  return {
    ...response,
    message: lines[lines.length - 1] ?? REVIEW_PROMPT,
  };
}

/** Gateway를 통해 현재 사용자의 커리큘럼 추천 대화를 호출한다. */
export const curriculumChatClient: ChatClient = {
  async send(request: ChatRequest): Promise<ChatResponse> {
    const response = await apiFetch<ChatResponse>(CHAT_PATH, {
      method: "POST",
      body: request,
    });
    return normalizeResponse(response);
  },

  reset(): Promise<void> {
    return apiFetch<void>(`${CHAT_PATH}/session`, { method: "DELETE" });
  },
};
