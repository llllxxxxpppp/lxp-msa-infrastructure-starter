// curriculum-service(FastAPI) POST /chat 의 요청·응답 계약을 그대로 옮긴 타입.
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
 * 봇의 CurriculumStep 파이단틱 모델에는 stage/course_id/reason 만 있지만,
 * 그건 LLM 구조화 출력용 스키마다. 응답에 실리는 draft_curriculum 은
 * _normalize_plan() 이 강좌 제목·소요시간을 채워 넣은 dict 라서 5개 필드가 온다.
 */
export interface CurriculumStep {
  stage: Stage;
  course_id: string;
  title: string;
  duration: string;
  reason: string;
}

export interface CurriculumPlan {
  summary: string;
  steps: CurriculumStep[];
}

export interface ChatRequest {
  thread_id: string;
  message: string;
}

export interface ChatResponse {
  thread_id: string;
  status: ChatStatus;
  message: string;
  user_profile: UserProfile;
  target_goal: string | null;
  missing_info: string[];
  curriculum: CurriculumPlan | null;
}

/** 컴포넌트와 통신 수단을 갈라놓는 이음매. 지금은 목만, 나중에 실제 봇 구현을 끼운다. */
export interface ChatClient {
  send(request: ChatRequest): Promise<ChatResponse>;
}

export const EMPTY_PROFILE: UserProfile = {
  job: null,
  experience: null,
  current_level: null,
};
