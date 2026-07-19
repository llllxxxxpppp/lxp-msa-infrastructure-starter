# member-service 실행 환경 테스트 결과 (docker compose)

이 문서는 저장소 루트(`lxp-msa-infrastructure-starter`)에서 `docker compose up`으로 전체 스택을 띄운 뒤, `member-service`가 구현한 기능(MEMBER-08~17: 내부 API, 상태 통지, 회원 정지, 시드 데이터, 이벤트 로깅, 헥사고날 전환, Swagger)을 실제 실행 환경에서 검증한 **결과 기록**이다.

**테스트 실행 일시**: 2026-07-19
**실행 방법**: 저장소 루트에서 `docker compose up -d` → 전 서비스 `healthy` 확인 후 아래 항목을 순서대로 `curl`/`docker compose logs`로 직접 실행.
**결론 요약**: member-service 자체 기능(1~5, 7, 8, 9)은 **전부 정상 동작**. 다만 **`compose.yaml`에 Course/Subscription 연동용 환경변수가 누락되어 있어 상태 통지가 항상 실패**하는 실제 버그를 발견했다(6번 항목, 아래 상세 참고). Gateway는 member-service에 대한 라우팅이 아직 전혀 설정되어 있지 않다(공개/내부 API 모두 404).

---

## 1) 인프라/기동 확인 — ✅ 전부 통과

- [x] `lxp-member-service`가 `healthy` (기동 후 약 25초 내 도달)
- [x] `/actuator/health` → `status: UP`, `db`/`consul`/`clientConfigServer`/`discoveryComposite` 전부 `UP`
- [x] Consul 카탈로그(`GET /v1/catalog/service/member-service`)에 `member-service`가 `172.20.0.12:8082`로 정상 등록됨
- [x] 로그에 기동 에러 없음, `Started MemberServiceApplication` 확인

---

## 2) 시드 데이터 확인 (MEMBER-11) — ✅ 전부 통과

```
id=1: {"role":"ADMIN","suspended":false,"deleted":false}
id=2: {"role":"INSTRUCTOR","suspended":false,"deleted":false}
id=3: {"role":"MEMBER","suspended":false,"deleted":false}
by-email/instructor@lxp.local: {"memberId":2,"passwordHash":"{noop}placeholder-encoded-password","role":"INSTRUCTOR","suspended":false,"deleted":false}
```

- [x] id=1/2/3의 role이 정확히 ADMIN/INSTRUCTOR/MEMBER
- [x] 세 계정 모두 suspended/deleted가 false
- [x] 강사 고정 ID(2)가 by-email로도 정확히 조회됨

---

## 3) 공개 API — 관리자 (`/api/admin/members`) — ✅ 전부 통과

| 시나리오 | 실제 결과 |
|---|---|
| 강사 계정 생성 | `201 {"id":1000,"email":"...","role":"INSTRUCTOR"}` (자동 증가 ID가 1000부터 시작 — MEMBER-11의 `RESTART WITH 1000` 정상 반영 확인) |
| 동일 이메일 재생성 | `400 {"message":"이미 사용 중인 이메일 입니다."}` |
| 강사(id=1000) 정지 | `200`, 이후 auth-status: `{"role":"INSTRUCTOR","suspended":true,"deleted":true}` — **정지 시 deleted도 함께 true가 되는 도메인 규칙이 실제로도 그대로 동작함**(설계된 동작, 버그 아님) |
| 일반 회원(id=3)을 강사 정지 엔드포인트에 | `400 {"message":"강사가 아닙니다."}` |
| 일반 회원(id=3) 정지 (MEMBER-10) | `200`, auth-status: `{"role":"MEMBER","suspended":true,"deleted":true}` |
| 강사(id=1000)를 일반 회원 정지 엔드포인트에 | `400 {"message":"일반 회원이 아닙니다."}` |

---

## 4) 공개 API — 회원 자기 관리 (`/api/members/me`) — ✅ 전부 통과

새 강사 계정(id=1003, 실제 비밀번호 해싱 경로로 생성)과 새 일반 회원(id=1002, 내부 API로 생성)으로 검증:

| 시나리오 | 실제 결과 |
|---|---|
| 올바른 현재 비밀번호로 변경 | `204` |
| 틀린 현재 비밀번호 | `400 {"message":"현재 비밀번호가 일치하지 않습니다."}` |
| `X-Member-Id` 헤더 누락 | `400 {"timestamp":...,"status":400,"error":"Bad Request",...}` — Spring 기본 응답(이 서비스가 자체 401/403을 만들지 않음, 인증/인가는 Gateway 책임이라는 설계와 일치) |
| 강사 프로필 수정 | `200 {"id":1003,"email":"...","role":"INSTRUCTOR"}` |
| 탈퇴(DELETE, id=1002) | `204`, 이후 auth-status: `{"role":"MEMBER","suspended":false,"deleted":true}` — **탈퇴는 정지와 달리 suspended는 그대로 false, deleted만 true** (설계된 동작대로 구분됨) |

---

## 5) 내부 API (`/internal/members`, MEMBER-08) — ✅ 전부 통과

| 시나리오 | 실제 결과 |
|---|---|
| `POST /internal/members`에 임의 문자열을 passwordHash로 전달 | `{"memberId":1004}` → `by-email` 재조회 시 `"passwordHash":"UNIQUE_MARKER_HASH_VALUE_12345"` — **입력한 값이 재인코딩 없이 정확히 그대로 저장/반환됨(§5 핵심 요구사항 실제 확인)** |
| 존재하지 않는 이메일 조회 | `400 {"message":"존재하지 않는 회원입니다."}` |
| 존재하지 않는 ID(999999) auth-status | `400 {"message":"존재하지 않는 회원입니다."}` |
| 일반 회원(id=1002, 탈퇴됨) suspension-status | `200 {"suspended":false}` — 강사 여부 검증 없이 정상 동작 |
| 강사(id=1003) suspension-status | `200 {"suspended":false}` |

**Gateway 경유 노출 여부**: `curl http://localhost:8080/internal/members/1/auth-status` → **`404 Not Found`(Spring Cloud Gateway 기본 404 포맷)**. 결과만 보면 "차단됨"이지만, 아래 참고사항대로 **공개 API도 Gateway에서 동일하게 404**가 나서 내부 API만 특별히 차단된 것이 아니라 **Gateway에 member-service 라우팅 자체가 아직 전혀 구성되어 있지 않은 것으로 보인다**(우연히 안전한 상태 — Gateway 담당자가 실제 라우팅을 추가할 때 `/internal/**` 제외 처리를 반드시 함께 넣어야 함).

---

## 6) 상태 변경 통지 (MEMBER-09/12) — ⚠️ 버그 발견: `compose.yaml` 환경변수 누락

- [x] INFO 로그(`... occurred. eventId=..., occurredAt=...`)는 정지/탈퇴/강사정지 각 동작마다 **항상 정상적으로 남음**(MEMBER-12 의도대로 동작).
- [x] 통지 실패해도 **member-service 자신의 응답(정지/탈퇴 200/204)은 영향받지 않음** — "무시+로그" 정책이 실제로도 정확히 동작함(가장 중요한 확인 포인트, 정상).
- [!] **하지만 통지가 100% 실패한다.** 로그 상세:
  ```
  InstructorSuspended notification failed. instructorId=1000, occurredAt=...
  org.springframework.web.client.ResourceAccessException: I/O error on POST request for
  "http://localhost:8083/internal/courses/by-instructor/1000/unpublish-all":
  Connect to http://localhost:8083 failed: Connection refused

  MemberSuspended notification failed. memberId=3, occurredAt=...
  ResourceAccessException: ... "http://localhost:8084/internal/subscriptions/by-member/3/suspend" ...
  Connection refused
  ```

**근본 원인**: `member-service`의 `application.yml`에 설정된 `member.notification.subscription-service.base-url`/`course-service.base-url`의 기본값(`http://localhost:8084`/`http://localhost:8083`)이 컨테이너 환경에서 그대로 쓰이고 있다. 컨테이너 내부의 `localhost`는 **member-service 자신**을 가리키므로, Subscription/Course가 아니라 자기 자신의 8083/8084 포트(리스닝하지 않음)로 연결을 시도해 항상 실패한다.

**직접 확인**: 저장소 루트 `compose.yaml`의 `member-service.environment` 블록에는 `CONSUL_HOST`/`CONSUL_PORT`/`LOG_DIR`/`CONFIG_SERVER_URL`/`ZIPKIN_ENDPOINT`만 있고 **`SUBSCRIPTION_SERVICE_URL`/`COURSE_SERVICE_URL`가 빠져 있다.**

또한 호스트에서 course-service(8083)/subscription-service(8084)에 직접 `/internal/courses/by-instructor/...`, `/internal/subscriptions/by-member/...`를 호출해본 결과 **두 서비스 모두 아직 해당 내부 API 자체가 없어 404**였다 — 즉 URL을 고쳐도 상대측 API가 아직 없으면 여전히 실패하지만(이건 Course/Subscription 팀 몫, MEMBER-09 완료 기준에 이미 문서화된 리스크), **URL 문제는 member-service 배포 설정(`compose.yaml`)의 실제 버그이며 지금 고칠 수 있는 부분이다.**

**제안하는 수정** (`compose.yaml`의 `member-service.environment`에 추가):
```yaml
      SUBSCRIPTION_SERVICE_URL: http://subscription-service:8084
      COURSE_SERVICE_URL: http://course-service:8083
```
(Docker Compose 네트워크 내부에서는 서비스명이 DNS로 해석되므로 `localhost` 대신 서비스명을 써야 한다.)

---

## 7) Swagger / OpenAPI (MEMBER-17) — ✅ 통과

- [x] `/v3/api-docs`에 3개 태그 전부 확인: `"관리자 - 회원 관리"`, `"회원 자기 관리"`, `"내부 API (Auth/Course 전용 - 외부 미노출)"`
- [x] `GET /swagger-ui/index.html` → `200`
- [x] Gateway 경유(`localhost:8080/swagger-ui/index.html`) → `404`(6번 항목과 동일하게 Gateway 라우팅 미구성으로 인한 것으로 보임 — Swagger를 의도적으로 차단한 것인지 판단하려면 Gateway 라우팅이 먼저 구성되어야 함)

---

## 8) 예외/에러 응답 형식 — ✅ 통과

`MemberException` 발생 케이스(이메일 중복, 비밀번호 불일치, 존재하지 않는 회원, 강사/일반회원 타입 불일치 등) 전부 `{"message": "..."}` 형식 + `400`으로 일관되게 응답함을 위 1~5번 테스트에서 확인.

---

## 9) 데이터 휘발성 — 참고 사항, 실행 확인 불필요

H2 인메모리 특성상 컨테이너 재기동 시 시드 3건 외 모든 테스트 데이터(이번에 만든 id=1000~1004 등)가 초기화된다. 실제로 이번 테스트에서 새로 생성한 계정들은 재현 가능하나 영구적이지 않음에 유의.

---

## 부록: 테스트 도구 관련 참고사항 (버그 아님)

- 한글이 포함된 JSON 바디를 `curl -d '...'`로 직접 전달하면 이 테스트 환경(Git Bash/Windows)의 로케일 문제로 UTF-8이 깨져 `400 Bad Request`가 날 수 있었다(실제 재현: 한글 이름으로 강사 생성 시도 → 400, 동일 요청을 UTF-8 파일로 저장 후 `--data-binary @file`로 재전송 → 정상 `201`). 실제 애플리케이션 문제가 아니라 테스트 명령 실행 환경의 인코딩 이슈이므로, 한글 데이터로 재현 테스트 시 `--data-binary @file.json`(UTF-8로 저장된 파일) 방식을 권장한다.
