# LXP Frontend

`lxp-msa-infrastructure-starter`의 프론트엔드입니다. Next.js(App Router) + TypeScript + Tailwind CSS로 구성했고, 백엔드의 [Gateway](../gateway)(단일 진입점, 기본 `http://localhost:8080`)를 통해서만 API를 호출합니다.

다른 하위 폴더(Gradle 서비스들)와 마찬가지로 **완전히 독립된 프로젝트**입니다. 루트에는 이 프로젝트를 위한 별도 설정이 없습니다.

## 실행 방법

1. 백엔드가 떠 있어야 합니다 (최소 gateway + consul + config-server + 사용할 서비스). 루트 `README.md`의 "실행 방법"을 참고하세요.
2. 환경 변수 설정:
   ```bash
   cp .env.local.example .env.local
   ```
   기본값(`NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`)이 docker compose 기준과 맞으면 그대로 두면 됩니다.
3. 의존성 설치 및 개발 서버 실행:
   ```bash
   npm install
   npm run dev
   ```
   기본 포트는 **3000**입니다. (루트 `compose.yaml`에서 Grafana를 3001로 옮겨 이 포트를 프론트엔드용으로 비워뒀습니다.)

## 폴더 구조

```
src/
├─ app/                     # App Router
│  ├─ (auth)/               # 로그인 불필요: /login, /signup
│  ├─ (main)/               # 로그인 필요: /courses, /courses/[courseId], /members, /subscriptions, /curriculum-recommendation
│  ├─ (admin)/              # ROLE_ADMIN 전용: /policy-explorer
│  └─ layout.tsx
├─ components/
│  ├─ ui/                   # Button/Input/Card/Chip/Avatar/ProgressBar/Table/MaterialIcon
│  ├─ layout/               # AppHeader, AuthGuard, RoleGuard
│  ├─ admin/                # AdminSidebar
│  ├─ chat/                 # CourseChatWidget (정적 목업)
│  ├─ curriculum/           # CurriculumChat — 커리큘럼 추천 대화 화면
│  └─ policy/               # PolicyAiAssistModal (정적 목업)
├─ features/                 # 백엔드 서비스 경계와 1:1로 맞춘 도메인별 API/훅
│  ├─ auth/                  # auth-service (/api/auth/**)
│  ├─ member/                 # member-service (/api/members/**)
│  ├─ course/                  # course-service (/api/courses/**)
│  ├─ subscription/             # subscription-service (/api/subscriptions/**)
│  └─ curriculum/               # curriculum-service (POST /chat) — 미연결, 목 클라이언트
├─ lib/
│  ├─ api-client.ts           # fetch 래퍼 + 401 인터셉터(재발급)
│  ├─ token-storage.ts         # access/refresh 토큰 저장
│  ├─ jwt.ts                    # accessToken payload 디코드(표시 전용)
│  └─ env.ts                     # 환경변수 검증
└─ types/                         # 서버 공통 타입 (에러 응답 등)
```

새 도메인 화면을 추가할 때는 `features/<domain>/{api.ts,types.ts,hooks.ts}`를 먼저 만들고, `app/(main)/<domain>/` 아래에 페이지를 붙이는 순서를 따르면 기존 구조와 일관됩니다.

`design/`(Stitch 디자인 export)을 실제 화면으로 옮기는 작업 계획과 진행 상황은 [task.md](task.md), [CHECKLIST.md](CHECKLIST.md)를 참고하세요.

## 인증 흐름

`gateway/GATEWAY_MIGRATION_PLAN.md`의 "A. 프론트엔드 인터셉터" 방식을 따릅니다.

- 로그인(`POST /api/auth/login`) 성공 시 `accessToken`/`refreshToken`을 받아 `lib/token-storage.ts`에 저장합니다 (access token은 메모리 우선 + localStorage 보조, refresh token은 localStorage).
- 이후 모든 요청은 `lib/api-client.ts`의 `apiFetch`를 통해 나가며, `Authorization: Bearer <accessToken>` 헤더를 자동으로 붙입니다.
- 401 응답을 받으면 `POST /api/auth/refresh`를 `X-Refresh-Token` 헤더로 호출해 재발급을 시도하고, 성공하면 원래 요청을 새 토큰으로 한 번 재시도합니다. 동시에 여러 요청이 401을 받아도 재발급 요청은 한 번만 나갑니다.
- 재발급도 실패하면 토큰을 지우고 에러를 그대로 던집니다 — 화면에서는 이 경우 로그인 페이지로 보내면 됩니다.

**알려진 제약**: 토큰을 localStorage에 저장하기 때문에 Next.js의 Edge `middleware.ts`로는 로그인 여부를 판별할 수 없습니다(Edge는 localStorage에 접근 불가). 그래서 `(main)`/`(admin)` 라우트 그룹은 서버 미들웨어가 아니라 클라이언트 컴포넌트인 `components/layout/AuthGuard.tsx`(로그인 여부)와 `components/layout/RoleGuard.tsx`(역할 — `(admin)`에서 `ROLE_ADMIN` 체크)로 가드합니다. 두 가드 모두 `lib/jwt.ts`로 accessToken payload를 디코드해 판단하며, 이는 UI 편의용일 뿐 실제 인가는 항상 게이트웨이/서비스가 다시 검증합니다. 추후 보안을 강화하며 httpOnly 쿠키 기반 BFF(Route Handler가 게이트웨이를 대신 호출)로 전환하면, 이 가드를 `middleware.ts`로 옮길 수 있습니다.

## 백엔드 API 계약 메모

각 `features/*/api.ts`, `types.ts`는 해당 백엔드 서비스의 실제 Controller/DTO를 보고 맞췄습니다. 백엔드가 바뀌면 아래 파일 쌍을 함께 확인하세요.

| 프론트                    | 백엔드                                                                                             |
| ------------------------- | -------------------------------------------------------------------------------------------------- |
| `features/auth/*`         | `auth-service/src/main/java/com/lcs/auth/controller/AuthController.java`                           |
| `features/member/*`       | `member-service/src/main/java/com/lcs/member/presentation/MemberController.java`                   |
| `features/course/*`       | `course-service/src/main/java/com/lcs/course/presentation/CourseController.java`                   |
| `features/subscription/*` | `subscription-service/src/main/java/com/lcs/subscription/presentation/SubscriptionController.java` |
| `features/curriculum/*`   | `curriculum-service/app/api/chat_controller.py` (`feature/curriculum-service` 브랜치)              |

주의: 4개 서비스의 에러 응답은 각각 `record ErrorResponse(String message)`로 **개별 정의**되어 있고(공통 모듈로 통일되지 않음), `status`/`code`/`timestamp` 같은 필드는 없습니다. `types/api.ts`의 `ApiErrorResponse`도 이에 맞춰 `message` 하나만 갖습니다.

## 스크립트

```bash
npm run dev            # 개발 서버 (http://localhost:3000)
npm run build           # 프로덕션 빌드
npm run start            # 빌드 결과 실행
npm run lint              # ESLint
npm run format             # Prettier로 포맷
npm run format:check        # 포맷 여부만 검사 (CI용)
```

## 커리큘럼 추천 봇 (미연결)

`/curriculum-recommendation` 화면은 커리큘럼 추천봇과 대화하는 UI지만, **아직 봇을 호출하지 않고 목 클라이언트로 동작합니다.**

봇(`curriculum-service`)은 `feature/curriculum-service` 브랜치에서 이 저장소로 들어와 compose에 등록돼 있습니다(호스트 `8001`). 프론트엔드는 게이트웨이만 호출하는데 **게이트웨이에 이 봇으로 가는 라우트가 없어서** 부를 방법이 없습니다. `/api/ai/**`는 `ai-tutor-service`(강의 PDF 챗봇)가 쓰고 있어 경로도 겹치지 않게 새로 정해야 합니다.

### 실연동할 때 할 일

1. **게이트웨이 라우트 추가** — 봇의 실제 경로가 prefix 없는 `POST /chat`이라 `RewritePath` 필터가 필요합니다.

   ```yaml
   - id: curriculum-service
     uri: "http://${CURRICULUM_BOT_HOST:localhost}:${CURRICULUM_BOT_PORT:8001}"
     predicates:
       - Path=/api/curriculum/**
     filters:
       - RewritePath=/api/curriculum/(?<segment>.*), /$\{segment}
   ```

2. **`ChatClient` 구현 교체** — `app/(main)/curriculum-recommendation/page.tsx`의 `createMockChatClient()` 자리에 `apiFetch`를 쓰는 구현을 넣으면 됩니다. `features/curriculum/types.ts`의 인터페이스만 지키면 컴포넌트는 손대지 않습니다.

   ```ts
   export interface ChatClient {
     send(request: ChatRequest): Promise<ChatResponse>;
   }
   ```

3. **응답 본문의 커리큘럼 중복 제거** — 봇은 커리큘럼을 `curriculum` 필드로 내려주면서 `_render_curriculum()`으로 렌더링한 같은 내용을 `message` 본문에도 넣습니다. 화면은 `curriculum`으로 카드를 그리므로 그대로 두면 같은 내용이 두 번 보입니다.

CORS는 게이트웨이의 `CorsConfig`가 처리하므로 봇에 미들웨어를 추가할 필요는 없습니다.

### 목 클라이언트

`features/curriculum/mockClient.ts`는 턴을 세어 상태를 옮깁니다 — 인터뷰 2턴 → 커리큘럼 제시 → (승인이면 확정 / 아니면 재제안). 승인 판정은 봇 `feedback_node`의 규칙 기반 문구 목록을 그대로 옮겼고, 확정 뒤에 다시 말을 걸면 검토로 돌아가는 것도 봇의 `route_from_start`와 맞췄습니다.

강좌 데이터는 봇에 하드코딩된 12건을 `features/curriculum/courseCatalog.ts`에 복제했습니다. 화면 설계는 `design/curriculum-recommand/code.html`을 따릅니다.
