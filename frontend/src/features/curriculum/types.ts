// curriculum-service(FastAPI) POST /api/curriculum/chat 의 요청·응답 계약.
// 봇 계약이 바뀌면 이 파일만 고치면 된다.

export type ChatStatus = "interviewing" | "reviewing" | "completed";

export type Stage = "입문" | "실전" | "심화";

export const STAGES: readonly Stage[] = ["입문", "실전", "심화"];

export interface UserProfile {
  job: string | null;
  experience: string | null;
  current_level: string | null;
}

/**
 * 봇의 LLM 구조화 출력에 강좌 정보를 보강한 draft_curriculum의 한 단계다.
 */
export interface CurriculumStep {
  stage: Stage;
  course_id: number;
  title: string;
  duration_minutes: number;
  reason: string;
}

export interface CurriculumPlan {
  summary: string;
  steps: CurriculumStep[];
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  status: ChatStatus;
  message: string;
  user_profile: UserProfile;
  target_goal: string | null;
  missing_info: string[];
  curriculum: CurriculumPlan | null;
}

/** 컴포넌트와 curriculum-service 통신 수단을 분리하는 인터페이스. */
export interface ChatClient {
  send(request: ChatRequest): Promise<ChatResponse>;
  reset(): Promise<void>;
}

export const EMPTY_PROFILE: UserProfile = {
  job: null,
  experience: null,
  current_level: null,
};
