# member-service

회원(Member) 바운디드 컨텍스트 서비스. Lxp-backend 모놀리스에서 도메인 코드를 이관했으며, 이후 MSA 내부 API·상태 통지·헥사고날(포트/어댑터) 아키텍처 전환·Swagger 문서화까지 완료했다.

- Port: `8082`
- Application: `com.lcs.member.MemberServiceApplication`
- Health: `http://localhost:8082/actuator/health`
- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- OpenAPI 스펙: `http://localhost:8082/v3/api-docs`

전체 스택 실행 방법(Docker Compose, IntelliJ 개별 실행 등)은 저장소 루트 [`README.md`](../README.md)를 참고한다.

## 책임 범위

로그인/회원가입/JWT 발급은 Auth 서비스, 역할 기반 인가는 API Gateway가 전담한다. member-service는 Gateway가 이미 검증한 신원을 `X-Member-Id` 등 신뢰 헤더로 전달받아, 다음만 담당한다:

- 회원 자기 정보 관리(비밀번호 변경, 강사 프로필 수정, 자진 탈퇴)
- 관리자 강사 관리(강사 계정 생성, 강사/일반 회원 정지)
- Auth/Course가 호출하는 내부 API 제공
- 회원 상태 변경 시 Course/Subscription에 대한 동기 HTTP 통지

## 아키텍처

순수 헥사고날(포트/어댑터) 구조로 전환 완료(`domain`은 다른 계층을 참조하지 않음, `infrastructure`가 `domain`이 정의한 포트를 구현). 상세 구조와 전환 이력은 [`.claude/HEXAGONAL-ARCHITECTURE.md`](.claude/HEXAGONAL-ARCHITECTURE.md), [`.claude/ARCHITECTURE.md`](.claude/ARCHITECTURE.md) 참고.

```text
com.lcs.member
├─ presentation      — REST 컨트롤러, 예외 핸들러
├─ application       — Service, DTO
├─ domain            — 순수 POJO 엔티티/VO, 포트 인터페이스(리포지토리/알림), 예외, 이벤트
├─ infrastructure     — 포트 구현체(JPA 영속성 어댑터, HTTP 알림 어댑터)
└─ config            — Bean 설정
```

## API

### 공개 API — 회원 자기 관리 (`/api/members/me`, `X-Member-Id` 헤더 필요)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `PATCH` | `/password` | 비밀번호 변경 |
| `PATCH` | `/instructor-profile` | 강사 프로필 수정 |
| `DELETE` | `/` | 자진 탈퇴 |

### 공개 API — 관리자 (`/api/admin/members`)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/instructors` | 강사 계정 생성 |
| `POST` | `/instructors/{instructorId}/suspend` | 강사 정지 |
| `POST` | `/{memberId}/suspend` | 일반 회원 정지 |

### 내부 API (`/internal/members`) — Auth/Course 전용, 외부 미노출

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/` | 회원가입(이미 해싱된 비밀번호를 받아 그대로 저장) |
| `GET` | `/by-email/{email}` | 이메일로 자격증명 조회(Auth의 로그인 매칭용) |
| `GET` | `/{memberId}/auth-status` | 역할/정지/탈퇴 상태 조회(Auth의 토큰 재발급용) |
| `GET` | `/{instructorId}/suspension-status` | 강사 정지 여부 조회(Course의 2차 방어용) |

이 경로들은 Gateway 외부에 노출되면 안 된다(§`Msa-Conversion-member.md` §3.2 확정 사항). Swagger UI에서도 별도 태그(`내부 API (Auth/Course 전용 - 외부 미노출)`)로 구분되어 있다.

### 기타

- `GET /api/members/ping` — 단순 헬스체크

## 문서

- [`.claude/domain/MEMBER.md`](.claude/domain/MEMBER.md) — 도메인 규칙(불변식) 정의
- [`.claude/Msa-Conversion-member.md`](.claude/Msa-Conversion-member.md) — MSA 분리 설계·내부 API 계약
- [`.claude/ARCHITECTURE.md`](.claude/ARCHITECTURE.md) / [`.claude/HEXAGONAL-ARCHITECTURE.md`](.claude/HEXAGONAL-ARCHITECTURE.md) — 패키지 구조·헥사고날 전환
- [`.claude/TASK.md`](.claude/TASK.md) / [`.claude/task/task-member.md`](.claude/task/task-member.md) — 작업 이력(MEMBER-08~17)
- [`member-service-final-test.md`](member-service-final-test.md) — docker compose 실행 환경 통합 테스트 체크리스트/결과

## 알려진 후속 과제

- Gateway에 `/api/admin/members/**`, `/internal/members/**`, `/swagger-ui/**` 라우팅이 아직 없다(현재는 `/api/members/**`만 라우팅됨).
- `compose.yaml`의 member-service 환경변수에 `SUBSCRIPTION_SERVICE_URL`/`COURSE_SERVICE_URL`이 없어, Course/Subscription 상태 통지가 컨테이너 환경에서 항상 실패한다(무시+로그 정책으로 회원 상태 변경 자체는 정상 처리됨). 상세는 `member-service-final-test.md`의 6번 항목 참고.
- Course/Subscription 쪽에 상태 통지를 수신할 내부 API(`/internal/subscriptions/**`, `/internal/courses/by-instructor/**`)가 아직 구현되어 있지 않다(해당 팀 담당).
