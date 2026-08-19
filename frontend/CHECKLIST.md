# 구현 체크리스트

`task.md`의 작업 단위를 체크박스로 옮긴 문서. 완료되는 즉시 `[x]`로 갱신한다.

## 0. 디자인 시스템 (선행 작업)

- [x] `src/app/globals.css` — `DESIGN.md` 토큰을 Tailwind v4 `@theme` 블록으로 이식 (색상/radius/spacing/폰트사이즈)
- [x] `src/app/layout.tsx` — Geist → Inter(next/font/google), Material Symbols Outlined `<link>` 추가
- [x] `src/components/ui/Button.tsx` 재작업 (primary/secondary/ghost)
- [x] `src/components/ui/Input.tsx` 재작업 (아이콘 + focus glow)
- [x] `src/components/ui/Card.tsx` 재작업 (elevation)
- [x] `src/components/ui/Chip.tsx` (신규)
- [x] `src/components/ui/Avatar.tsx` (신규)
- [x] `src/components/ui/ProgressBar.tsx` (신규)
- [x] `src/components/ui/Table.tsx` (신규)
- [x] `src/components/ui/MaterialIcon.tsx` (신규)
- [x] `src/components/layout/AppHeader.tsx` (신규, `Header.tsx` 대체)
- [x] `src/app/(main)/layout.tsx` — `AppHeader` 적용
- [x] `src/lib/jwt.ts` (신규, accessToken payload 디코드)

## 1. 로그인 — `design/login/code.html`

- [x] `src/app/(auth)/login/page.tsx` 재작성

## 2. 회원가입 — `design/signup/code.html`

- [x] `src/app/(auth)/signup/page.tsx` 재작성

## 3. 강좌 목록 — `design/course/course-list/code.html`

- [x] `src/app/(main)/courses/page.tsx` 재작성 (검색/필터칩/정렬/카드/FAB)

## 4. 강좌 상세 (신규) — `design/course/course-detail/code.html`

- [x] `src/features/course/api.ts` — `getCourseDetail()` 추가
- [x] `src/features/course/types.ts` — `CourseDetail`/`Lecture`/`Mission` 추가
- [x] `src/app/(main)/courses/[courseId]/page.tsx` (신규)

## 5. 마이페이지 — `design/mypage/code.html`

- [x] `src/features/member/types.ts` — `ChangePasswordRequest`/`UpdateInstructorProfileRequest` 추가
- [x] `src/features/member/api.ts` — `changePassword()`/`updateInstructorProfile()`/`withdraw()` 추가
- [x] `src/app/(main)/members/page.tsx` 전면 재작성 (프로필/비번변경/강사프로필/탈퇴/학습이력·뱃지 MOCK)

## 6. 구독 — `design/subscription/code.html`

- [x] `src/app/(main)/subscriptions/page.tsx` 재작성 (현재플랜 조회/취소 실연동, 나머지 MOCK)

## 7. CourseChatWidget (정적) — `design/course-chatbot/*`

- [x] `src/components/chat/CourseChatWidget.tsx` (신규)
- [x] 강좌 상세 페이지에 마운트

## 8. 커리큘럼 추천 — `design/curriculum-recommand/code.html`

- [x] `src/app/(main)/curriculum-recommendation/page.tsx` (신규)
- [x] 강좌 목록 FAB / 강좌 상세 CTA에서 연결
- [x] `src/features/curriculum/{types,mockClient,courseCatalog,hooks}.ts` — 봇 계약과 목 클라이언트
- [x] `src/components/curriculum/*` — 채팅 컴포넌트 8개
- [x] 하드코딩 시나리오를 실제 대화형으로 교체
- [ ] 봇 실연동 — 게이트웨이 라우트가 없어 별도 작업

## 9. 정책 탐색기 어드민 (정적) — `design/policy-explorer/*`

- [x] `src/components/layout/RoleGuard.tsx` (신규, `AuthGuard` 확장)
- [x] `src/app/(admin)/layout.tsx` (신규, `RoleGuard` + `AdminSidebar` 적용)
- [x] `src/components/admin/AdminSidebar.tsx` (신규)
- [x] `src/app/(admin)/policy-explorer/page.tsx` (신규)
- [x] `src/components/policy/PolicyAiAssistModal.tsx` (신규)

## 검증

- [x] `npm run lint` 통과
- [x] `npm run build` 통과
- [ ] `DEV_RUN.md` 절차로 백엔드 기동 후 End-to-End 수동 확인 (로그인→강좌목록→강좌상세→마이페이지→구독) — 실제 백엔드를 띄운 환경에서 사람이 직접 확인 필요
