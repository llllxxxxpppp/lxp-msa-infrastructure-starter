# member-service

회원(Member) 바운디드 컨텍스트 서비스. Lxp-backend 모놀리스에서 도메인 코드를 이관했습니다(2026-07-19).

- Port: `8082`
- Application: `com.lcs.member.MemberServiceApplication`
- Health: `http://localhost:8082/actuator/health`

## 문서

- `.claude/domain/MEMBER.md` — 도메인 규칙(불변식) 정의
- `.claude/Msa-Conversion-member.md` — MSA 분리 설계·내부 API 계약
- `.claude/member-sv-plan.md` — 이관 이력 및 남은 작업 목록
- `.claude/HEXAGONAL-ARCHITECTURE.md` — 헥사고날(포트/어댑터) 전환 목표(착수 전)
- `.claude/CLAUDE.md` — AI 페어 프로그래밍(Claude Code) 참고 규칙

## API

- `GET /api/members/ping` — 헬스체크
- `POST /api/admin/members/instructors` — 강사 계정 생성(어드민)
- `POST /api/admin/members/instructors/{instructorId}/suspend` — 강사 정지(어드민)
- `PATCH /api/members/me/password` — 비밀번호 변경 (`X-Member-Id` 헤더 필요)
- `PATCH /api/members/me/instructor-profile` — 강사 프로필 수정 (`X-Member-Id` 헤더 필요)
- `DELETE /api/members/me` — 자진 탈퇴 (`X-Member-Id` 헤더 필요)

## 아직 미구현

- Auth/Course가 호출하는 내부 API 계약(`.claude/Msa-Conversion-member.md` §3.2/§4.3)
- Course/Subscription으로의 상태 변경 통지(§4.4)
- 일반 회원(비강사) 정지 엔드포인트
