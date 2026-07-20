# auth-service

JWT 기반 인증을 제공하는 마이크로서비스입니다. (기본 포트: `8081`)

## API 엔드포인트

| Method | Path | 설명 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/api/auth/ping` | 헬스 체크 | - | `{ service, status, timestamp }` |
| POST | `/api/auth/login` | 로그인 | Body `{ email, password }` | `{ accessToken, refreshToken }` |
| POST | `/api/auth/logout` | 로그아웃 | Header `X-Refresh-Token` (선택) | 204 No Content |
| POST | `/api/auth/refresh` | 액세스 토큰 재발급 | Header `X-Refresh-Token` (필수) | `{ accessToken }` |

`/api/auth/**`는 Spring Security에서 `permitAll` 처리되어 있으며, 오류 응답은 공통으로 `{ message }`(`ErrorResponse`) 형태로 반환됩니다.

## member-service 연동

로그인 시 `CustomUserDetailsService` → `MemberClient`가 회원 정보를 조회합니다. `MemberClient`는 인터페이스이며, `auth.member-client.mode` 설정(`AUTH_SERVICE_MEMBER_CLIENT_MODE` 환경변수, 기본값 `rest`)에 따라 구현체가 전환됩니다.

- `rest` (기본값, `RestMemberClient`)
  - `POST http://member-service/internal/members/by-email/info`
  - 요청 Body: `{ "email": "user@example.com" }`
  - 응답 Body: `{ "id": 1, "password": "<encoded>", "role": "USER", "suspended": false, "deleted": false }`
- `grpc` (`GrpcMemberClient`)
  - `member_login_info.proto`(`MemberLoginInfoService.GetMemberLoginInfo`)로 통신하며, 대상은 `auth.member-client.grpc.host`/`port`(`AUTH_SERVICE_MEMBER_CLIENT_GRPC_HOST`/`AUTH_SERVICE_MEMBER_CLIENT_GRPC_PORT`, 기본값 `member-service:9090`)로 설정합니다.

공통적으로 회원을 찾지 못하면(REST 404 / gRPC `NOT_FOUND`) 빈 결과로, 그 외 통신 오류는 `MemberServiceUnavailableException`을 거쳐 503으로 처리됩니다.

## 개발 도구

- H2 콘솔: `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:mem:lxp-test`, 사용자 `sa`, 비밀번호 없음)
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI 문서: `http://localhost:8081/v3/api-docs`
