# 프론트엔드 화면 구현 작업 계획 (Stitch 디자인 반영)

`frontend/design/`에 Google Stitch로 뽑은 정적 HTML/이미지 export가 11개 화면 있고(`DESIGN.md`에 색상/타이포/spacing/radius 디자인 토큰이 정의돼 있음), 현재 `frontend/src`는 기능은 있지만 스타일은 기본 회색 Tailwind라 이 디자인이 전혀 반영돼 있지 않다. 이 문서는 그 디자인을 실제 화면에 입히는 작업 순서를 정리한다.

## 화면별 백엔드 상태 (구현 범위를 정하는 기준)

| 화면                                        | 백엔드 상태                                                                                      |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| 로그인/회원가입                             | 완전히 존재 (`auth-service`, `member-service`)                                                   |
| 강좌 목록/강좌 상세                         | 완전히 존재 (`course-service` `GET /api/courses`, `GET /api/courses/{id}/detail`)                |
| 마이페이지                                  | 일부만 존재 (비번변경/강사프로필수정/탈퇴는 O, "내 프로필 조회"·학습이력·뱃지는 API 자체가 없음) |
| 구독                                        | 일부만 존재 (ID로 단건 조회/취소는 O, "내 구독" 조회·업그레이드·결제수단·청구내역은 API 없음)    |
| AI 챗봇, 커리큘럼 추천, 정책 탐색기(어드민) | **백엔드 전무** — AI/챗봇 서비스가 리포에 없고, `policy-explorer-service`는 완전히 빈 폴더       |

결정 사항:

- 백엔드가 아예 없는 3개 화면(AI 챗봇/커리큘럼 추천/정책 탐색기)도 **정적 목업으로 미리 배치**한다 (버튼/토글 등 로컬 상호작용만, 실제 API 호출 없음).
- 마이페이지·구독 중 대응 API가 없는 섹션(학습이력, 뱃지, 결제수단, 청구내역)은 **목업 데이터로 채워서** 디자인대로 보이게 하되, 코드에 `// MOCK` 주석으로 명시한다.

---

## 0. 디자인 시스템 반영 (선행 작업, 다른 모든 화면의 기반)

- `src/app/globals.css`: Tailwind v4 `@theme` 블록에 `DESIGN.md`의 토큰을 이식 — 색상 전체 팔레트(primary/secondary/surface 계열/on-surface/outline/tertiary/error/success-green/warning-amber/error-red/slate-text), radius 스케일(`sm .25rem / DEFAULT .5rem / md .75rem / lg 1rem / xl 1.5rem / full 9999px`), spacing(`stack-sm 8px/stack-md 16px/stack-lg 32px`, `gutter 24px`, `margin-mobile/desktop`), 폰트 사이즈 스케일(headline-lg/md/sm, body-lg/md/sm, label-md/sm).
- 폰트: `layout.tsx`의 Geist(next/font)를 Inter(next/font/google)로 교체. Material Symbols Outlined는 next/font 미지원이라 root layout의 `<head>`에 `<link>` 태그로 직접 로드(디자인 export와 동일 방식).
- `src/components/ui/*` 재작업: `Button`(primary/secondary/ghost — DESIGN.md 버튼 규칙 그대로), `Input`(좌측 아이콘 + 포커스 시 glow), `Card`(elevation 레벨 반영). 신규 컴포넌트: `Chip`(카테고리/상태 태그), `Avatar`, `ProgressBar`, `Table`, `MaterialIcon`(`<span class="material-symbols-outlined">` 래퍼).
- `src/components/layout/Header.tsx` → `AppHeader.tsx`로 교체: 로고 + "Course Categories"/"My Learning" 내비 + 검색창 + 알림벨(정적) + 아바타 드롭다운(마이페이지/로그아웃). `(main)/layout.tsx`에서 교체 적용.
- `src/lib/jwt.ts` 신규: accessToken payload를 base64 디코드해 `sub`(email)/`userId`/`roles`를 화면 표시용으로만 사용(서버 재검증 아님, 인가 목적 아님 — 실제 인가는 항상 게이트웨이/서비스가 담당). "내 프로필 조회" API가 없는 마이페이지에서 이메일/역할 표시에 사용.

## 1. 로그인 — `design/login/code.html`

`app/(auth)/login/page.tsx` 재작성: 로고 박스, 이메일/비밀번호 아이콘 인풋, 비밀번호 표시/숨김 토글(로컬 상태), Remember me 체크박스(로컬 UI만 — 저장 로직 없음을 주석으로 명시), 화살표 아이콘 제출 버튼. 기존 `features/auth/hooks.ts`의 `useLogin` 그대로 재사용, API 변경 없음.

## 2. 회원가입 — `design/signup/code.html`

`app/(auth)/signup/page.tsx` 재작성. 기존 `features/member/api.ts`의 `signup()` 그대로 재사용.

## 3. 강좌 목록 — `design/course/course-list/code.html`

`app/(main)/courses/page.tsx` 재작성.

- 검색창: 실제 `GET /api/courses?keyword=` 서버 파라미터에 연결(입력 디바운스).
- 카테고리/난이도 필터 칩: `CourseService.getCourses`가 `keyword`만 지원하고 카테고리 파라미터가 없으므로, 가져온 페이지 목록에 대한 **클라이언트 필터링**으로 구현(디자인의 체크박스 필터 UI는 그대로, 동작만 클라이언트 사이드). `size`를 늘려(예: 50) 필터 체감이 되게 하고, 이 한계는 코드 주석으로도 남긴다.
- 정렬 드롭다운: `rating` 필드가 백엔드에 없으므로 옵션을 "추천순(서버 기본)"/"제목순"/"학습시간순" 정도로 축소해 클라이언트 정렬.
- 카드: `title`/`category`/`difficulty`/`durationMinutes`/`status` 표시. `thumbnailUrl`은 시드 데이터가 존재하지 않는 더미 CDN URL이라 `<img onError>`로 로컬 placeholder 폴백.
- 우측 하단 FAB "커리큘럼 추천" 추가 → 8번(정적) 페이지로 이동.
- 카드 클릭 시 4번(신규) 상세 페이지로 이동.

## 4. 강좌 상세 (신규 라우트) — `design/course/course-detail/code.html`

- 신규 `app/(main)/courses/[courseId]/page.tsx`.
- `features/course/api.ts`에 `getCourseDetail(courseId)` 추가 (`GET /api/courses/{id}/detail`), `features/course/types.ts`에 `CourseDetail`/`Lecture`/`Mission` 타입 추가 — 실제 DTO 기준으로 필드까지 맞춘다:
  - `LectureResponse(lectureId, title, status, contentType, sortOrder)`
  - `MissionResponse(missionId, title, status, sortOrder)`
- 헤더(제목/카테고리/난이도/설명), 커리큘럼 목록(강의+미션을 `sortOrder`로 합쳐 리스트로 표시). 실제 영상 재생 인프라는 없으므로 디자인의 비디오 플레이어 자리는 "강의 자료 목록"으로 대체.
- "View Recommended Curriculum Path" CTA → 8번 페이지로 링크.
- 7번 `CourseChatWidget`(정적)을 이 페이지에 마운트.

## 5. 마이페이지 — `design/mypage/code.html`

`app/(main)/members/page.tsx` 재작성(현재의 "준비 중" 플레이스홀더 대체).

- 프로필 헤더: `lib/jwt.ts`로 디코드한 email/role 표시 (실명·아바타 이미지 API는 없으므로 이메일 이니셜 등으로 대체).
- Personal Information: 이메일(읽기전용, 토큰 기반) + 비밀번호 변경 폼 → `features/member/api.ts`에 `changePassword()` 추가 (`PATCH /api/members/me/password`, `{currentPassword, newPassword}`).
- 강사 전용 섹션(역할에 `INSTRUCTOR` 포함 시에만 노출): 프로필 수정 폼 → `updateInstructorProfile()` 추가 (`PATCH /api/members/me/instructor-profile`, `{name, profileImageUrl, introduction}`).
- 회원 탈퇴 버튼 → `withdraw()` 추가 (`DELETE /api/members/me`), 확인 다이얼로그 후 토큰 삭제 + 로그아웃 처리.
- Learning History / Achievement Badges 섹션: 대응 API가 없으므로 **목업 데이터**로 디자인 그대로 채우고, 컴포넌트 상단에 `// MOCK: 학습 이력/뱃지 API 없음. 준비되면 features/course 또는 member에 endpoint 추가 후 교체` 주석을 남긴다.

## 6. 구독 — `design/subscription/code.html`

`app/(main)/subscriptions/page.tsx` 재작성.

- Current Plan 카드: 실제 `GET /api/subscriptions/{id}` 연결. 단, 게이트웨이에 "내 구독 조회" 엔드포인트가 없다(`SubscriptionInternalController#by-member`는 `/internal/**`로 서비스 간 전용, 게이트웨이 미노출) — 기존처럼 ID 입력 방식을 디자인 레이아웃 안에 자연스럽게 녹여 임시로 유지하고, **이 백엔드 갭을 코드 주석으로 명시적으로 기록**(추후 `GET /api/subscriptions/me` 같은 엔드포인트가 생기면 대체).
- Cancel Plan 버튼: 실제 `POST /api/subscriptions/{id}/cancel` 연결.
- Upgrade Plan / Payment Method / Plan Usage / Billing History(인보이스 테이블): 대응 API가 전혀 없으므로 **목업 데이터**로 채우고 각 섹션에 `// MOCK` 주석.

## 7~9. 정적 목업 화면 (백엔드 전무, 로컬 상호작용만)

- **CourseChatWidget** (`src/components/chat/CourseChatWidget.tsx`) — `design/course-chatbot/{chatbot-buttoon,chatbot-chat-screen}`. 열림/닫힘은 실제 로컬 상태로 토글되지만, 대화 내용은 하드코딩된 스크립트(실 API 호출 없음). 4번 강좌 상세 페이지에 마운트.
- **커리큘럼 추천** (`app/(main)/curriculum-recommendation/page.tsx`) — `design/curriculum-recommand`. 하드코딩된 다단계 챗 시나리오(버튼 클릭으로 다음 단계 진행), 실제 API 호출 없음.
- **정책 탐색기 어드민** (`app/(admin)/policy-explorer/page.tsx` + `PolicyAiAssistModal` 컴포넌트) — `design/policy-explorer/{main,modal}`. `ROLE_ADMIN` 전용이므로 `AuthGuard`를 역할 옵션을 받는 `RoleGuard`로 확장해 게이팅(역할 판정은 `lib/jwt.ts`로 디코드한 role 사용). 파일 목록/AI 모달 전부 정적 목업 데이터.

## 구현 순서

`0(디자인 시스템) → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9` 순으로 진행한다. 뒤로 갈수록 실제 백엔드 의존도가 낮아진다.

## 검증 방법

- 각 화면을 `npm run dev`로 직접 열어 `design/*/screen.png`(단, 5개는 깨진 placeholder라 이 경우 `code.html`을 기준으로)와 육안 비교.
- 실 API 연동 화면(1~4, 5의 비밀번호변경/탈퇴, 6의 조회/취소)은 루트 `DEV_RUN.md` 절차대로 백엔드를 띄우고 End-to-End로 확인.
- `npm run lint`, `npm run build` 통과 확인.
