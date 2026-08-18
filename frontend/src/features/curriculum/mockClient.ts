import { COURSE_CATALOG, type CatalogCourse } from "./courseCatalog";
import {
  EMPTY_PROFILE,
  STAGES,
  type ChatClient,
  type ChatRequest,
  type ChatResponse,
  type CurriculumPlan,
  type CurriculumStep,
  type Stage,
  type UserProfile,
} from "./types";

/*
 * 봇 없이 화면을 돌리기 위한 목 클라이언트.
 *
 * 고정 응답을 돌려주는 대신 턴을 세어 상태를 옮긴다. 와이어프레임이 검증하려는 것이
 * "interviewing → reviewing → completed 전이가 화면에서 말이 되는가" 이기 때문이다.
 *
 *   1턴  interviewing  직무를 받고 경력을 되묻는다
 *   2턴  interviewing  경력을 받고 현재 수준을 되묻는다
 *   3턴  reviewing     커리큘럼 초안 제시
 *   4턴~ 분기          승인이면 completed, 아니면 reviewing 으로 재제안
 *
 * 4턴 이후로도 계속 분기하는 이유는 봇의 route_from_start 가 초안이 있으면 무조건
 * 피드백 노드로 보내기 때문이다. 확정한 뒤에 말을 걸어도 다시 검토로 돌아간다.
 *
 * 실제 봇으로 갈아끼울 때 주의할 점: 봇은 커리큘럼을 message 본문에도 통째로 렌더링해
 * 넣는다(_render_curriculum). 목은 카드와 겹치지 않게 마무리 질문만 message 에 담으므로,
 * 실제 클라이언트를 붙이면 본문에서 커리큘럼 부분을 걷어내야 카드가 두 번 보이지 않는다.
 */

export interface MockClientOptions {
  /** 응답 지연. 로컬 LLM 은 실제로 수 초 이상 걸려서 기본값을 넉넉히 둔다. */
  latencyMs?: number;
  /** true 를 돌려주면 그 요청을 실패시킨다. 데모 화면의 오류 토글이 쓴다. */
  shouldFail?: () => boolean;
}

/** 봇 feedback_node 의 규칙 기반 분류를 그대로 옮겼다. */
const NEGATIVE_PHRASES = ["안좋", "별로", "수정", "바꿔", "변경", "다른"];
const APPROVAL_PHRASES = ["좋아", "좋습니다", "괜찮", "동의", "확정", "진행"];

function isApproval(message: string): boolean {
  const normalized = message.toLowerCase().replace(/\s+/g, "");
  const hasNegative = NEGATIVE_PHRASES.some((word) => normalized.includes(word));
  const hasApproval = APPROVAL_PHRASES.some((word) => normalized.includes(word));
  return !hasNegative && hasApproval;
}

/** 대화에서 카테고리를 골라내는 자리. 봇은 BM25 로 하지만 목은 단어 몇 개만 본다. */
const CATEGORY_KEYWORDS: { category: string; words: string[] }[] = [
  { category: "백엔드 개발", words: ["백엔드", "서버", "api", "아키텍처", "설계"] },
  { category: "데이터 분석", words: ["데이터", "sql", "분석", "지표", "대시보드"] },
  { category: "마케팅", words: ["마케팅", "캠페인", "광고", "퍼널"] },
  { category: "프로덕트", words: ["프로덕트", "기획", "제품", "pm"] },
];

function pickCategory(answers: string[]): string {
  const text = answers.join(" ").toLowerCase();
  let best = CATEGORY_KEYWORDS[0];
  let bestHits = 0;
  for (const entry of CATEGORY_KEYWORDS) {
    const hits = entry.words.filter((word) => text.includes(word)).length;
    if (hits > bestHits) {
      best = entry;
      bestHits = hits;
    }
  }
  return best.category;
}

function courseFor(category: string, stage: Stage): CatalogCourse {
  const match = COURSE_CATALOG.find(
    (course) => course.category === category && course.difficulty === stage,
  );
  // 카탈로그는 카테고리마다 세 난이도를 모두 갖고 있다. 못 찾으면 데이터가 깨진 것이다.
  if (!match) {
    throw new Error(`목 카탈로그에 ${category} ${stage} 강좌가 없습니다.`);
  }
  return match;
}

const INITIAL_REASONS: Record<Stage, string> = {
  입문: "본격적으로 들어가기 전에 기본 개념을 정리하는 단계입니다.",
  실전: "배운 내용을 실제로 만들어 보며 손에 익히는 단계입니다.",
  심화: "앞 단계를 확장해 혼자서도 문제를 풀 수 있게 하는 단계입니다.",
};

const ADJUSTED_REASONS: Record<Stage, string> = {
  입문: "이미 아시는 부분은 건너뛰고 필요한 절만 골라 들으셔도 됩니다.",
  실전: "전체를 다 듣기보다 프로젝트 파트 위주로 보시면 시간을 줄일 수 있습니다.",
  심화: "앞 두 단계를 마친 뒤에 시작하셔도 늦지 않습니다.",
};

function toStep(course: CatalogCourse, stage: Stage, reason: string): CurriculumStep {
  return {
    stage,
    course_id: course.id,
    title: course.title,
    duration: course.duration,
    reason,
  };
}

function initialPlan(category: string): CurriculumPlan {
  return {
    summary: `${category} 분야에서 기초를 다지고 실제로 만들어 본 뒤 확장까지 이어지는 3단계로 구성했습니다.`,
    steps: STAGES.map((stage) => toStep(courseFor(category, stage), stage, INITIAL_REASONS[stage])),
  };
}

/*
 * 재제안은 강의를 바꾸지 않고 설명만 다시 쓴다. 봇의 replan 동작과 같다.
 *
 * 카탈로그에는 카테고리마다 난이도별 강좌가 하나뿐이라 같은 분야에서 다른 강의를 고를
 * 수가 없다. 전체에서 제일 짧은 강의를 집어오면 백엔드 질문에 마케팅 강의를 물어오게
 * 되므로, 후보를 유지한 채 사유만 다시 쓰는 쪽이 정직하다.
 */
function adjustedPlan(category: string): CurriculumPlan {
  return {
    summary:
      "같은 분야에서 더 짧은 대안이 없어 강의는 그대로 두고, 단계마다 시간을 줄이는 방법을 적었습니다.",
    steps: STAGES.map((stage) =>
      toStep(courseFor(category, stage), stage, ADJUSTED_REASONS[stage]),
    ),
  };
}

interface ThreadState {
  answers: string[];
  profile: UserProfile;
  targetGoal: string | null;
  category: string | null;
  curriculum: CurriculumPlan | null;
}

const REVIEW_PROMPT = "이 커리큘럼이 괜찮으신가요? 바꾸고 싶은 부분이 있다면 말씀해 주세요.";

export function createMockChatClient(options: MockClientOptions = {}): ChatClient {
  const { latencyMs = 800, shouldFail = () => false } = options;
  const threads = new Map<string, ThreadState>();

  function threadState(threadId: string): ThreadState {
    let state = threads.get(threadId);
    if (!state) {
      state = {
        answers: [],
        profile: { ...EMPTY_PROFILE },
        targetGoal: null,
        category: null,
        curriculum: null,
      };
      threads.set(threadId, state);
    }
    return state;
  }

  function respond(
    request: ChatRequest,
    state: ThreadState,
    fields: Pick<ChatResponse, "status" | "message" | "missing_info">,
  ): ChatResponse {
    return {
      thread_id: request.thread_id,
      user_profile: { ...state.profile },
      target_goal: state.targetGoal,
      curriculum: state.curriculum,
      ...fields,
    };
  }

  return {
    async send(request: ChatRequest): Promise<ChatResponse> {
      await new Promise((resolve) => setTimeout(resolve, latencyMs));
      if (shouldFail()) {
        throw new Error("봇 응답을 받지 못했습니다.");
      }

      const state = threadState(request.thread_id);

      // 초안이 나온 뒤에는 모든 메시지가 피드백이다. 확정한 뒤에도 마찬가지다.
      if (state.curriculum) {
        if (isApproval(request.message)) {
          return respond(request, state, {
            status: "completed",
            message: "좋습니다. 이 커리큘럼을 최종 학습 로드맵으로 확정하겠습니다.",
            missing_info: [],
          });
        }
        state.curriculum = adjustedPlan(state.category ?? pickCategory(state.answers));
        return respond(request, state, {
          status: "reviewing",
          message: REVIEW_PROMPT,
          missing_info: [],
        });
      }

      state.answers.push(request.message);

      // 목은 답변을 해석하지 않는다. 받은 말을 그대로 해당 칸에 넣는다.
      if (state.answers.length === 1) {
        state.profile.job = request.message;
        return respond(request, state, {
          status: "interviewing",
          message: "현재 실무 경력은 어느 정도 되시나요?",
          missing_info: ["experience", "current_level"],
        });
      }

      if (state.answers.length === 2) {
        state.profile.experience = request.message;
        return respond(request, state, {
          status: "interviewing",
          message: "지금 어느 정도까지 직접 해보셨는지 알려주시겠어요?",
          missing_info: ["current_level"],
        });
      }

      state.profile.current_level = request.message;
      state.targetGoal = state.answers[0];
      state.category = pickCategory(state.answers);
      state.curriculum = initialPlan(state.category);
      return respond(request, state, {
        status: "reviewing",
        message: REVIEW_PROMPT,
        missing_info: [],
      });
    },
  };
}
