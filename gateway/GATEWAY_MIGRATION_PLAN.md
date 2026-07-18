# Gateway JWT 검증 이식 작업 플랜

- 모놀리식 `Lxp-backend`의 인증 코드를 `lxp-msa-infrastructure-starter`의 `gateway` 모듈로 이식하는 작업 계획
- 담당: API Gateway
- 방식: Spring Cloud Gateway (WebFlux) + GlobalFilter + jjwt

## 배경 / 아키텍처

```text
           ┌──────────────────────────────────────────────┐
           │  API Gateway — 매 요청 JWT 검증 (무상태)          │
           │  JwtAuthenticationFilter · validateToken     │
           └──────────────────────────────────────────────┘
                          │ userId·roles 헤더 전달 (신뢰 경계)
        ┌─────────────┬───┴────────┬──────────────┐
        ▼             ▼            ▼              ▼
   ┌────────┐   ┌─────────┐  ┌────────┐   ┌──────────────┐
   │  Auth  │   │ Member  │  │ Course │   │ Subscription │
   └────────┘   └─────────┘  └────────┘   └──────────────┘
```

### 확정된 결정
- **구현 방식**: `GlobalFilter` + jjwt 직접 구현 (spring-security 미사용)
  - 근거: 게이트웨이 역할이 "무상태 토큰 검증 + userId·roles 헤더 전달"로 명확. 경로별 세부 role 인가는 각 서비스에 위임하므로 Spring Security의 선언적 인가 이점이 불필요. 무게·학습비용 대비 이득이 큼.
- **만료 토큰 처리**: Gateway는 401만 반환. 재발급(refresh)은 auth-service가 담당 (무상태 게이트웨이 유지).
- **인가 위치**: Gateway는 인증(토큰 유효성)만. 경로별 세부 ROLE 인가는 각 도메인 서비스가 소유.

### 왜 그대로 이식이 안 되는가
- 모놀리식은 **서블릿(Spring MVC / Tomcat)** 기반: `OncePerRequestFilter`, `HttpServletRequest`, `filterChain.doFilter()`(void).
- 스타터 gateway는 **리액티브(Spring WebFlux / Netty)** 기반: `GlobalFilter`, `ServerWebExchange`, `Mono<Void>` 반환.
- 근거: `gateway/build.gradle` → `spring-cloud-starter-gateway-server-webflux` (Spring Cloud Gateway는 WebFlux 전용).
- 따라서 **필터 골격은 재작성**, 그 안의 **JWT 검증 로직(jjwt 호출)은 재사용**.

---

## 0. 스코프

**Gateway가 하는 일**
- 모든 요청에서 JWT 서명·만료 검증 (무상태)
- 검증 성공 → `X-User-Id`, `X-Role` 헤더 주입 후 다운스트림 전달
- 검증 실패/누락/만료 → 401
- 공개 경로(`/api/auth/**`, actuator 등)는 검증 없이 통과
- CORS 중앙 처리

**Gateway가 안 하는 일**
- 토큰 발급·리프레시·로그인 → auth-service
- 경로별 세부 ROLE 인가 → 각 도메인 서비스

---

## 1. 모놀리식 자산 분류

| 모놀리식 파일 | Gateway 처리 |
|---|---|
| `JwtTokenProvider` (`validateToken`, `parseClaims`, `resolveToken`, 키 초기화) | ✅ 검증 부분만 이식 |
| `JwtTokenProvider` (`createAccessToken/RefreshToken`, refresh validity) | ❌ auth-service로 |
| `JwtAuthenticationFilter` (OncePerRequestFilter) | ♻️ GlobalFilter로 재작성 (로직 참고) |
| `CustomUserPrincipal` | ❌ Gateway 불필요 (다운스트림이 헤더로 재구성) |
| `SecurityConfig`의 CORS 설정 | ♻️ 리액티브 `CorsWebFilter`로 재작성 |
| `SecurityConfig`의 authz 규칙 | ❌ 각 서비스로 |
| `CustomAuthenticationEntryPoint/AccessDeniedHandler` | ♻️ 401 JSON 응답 로직만 필터에 반영 |
| `RefreshService`, `RefreshToken*`, `CustomUserDetailsService` | ❌ auth-service로 |

---

## 2. 구현 단계

### Step 1 — 의존성 추가 (`gateway/build.gradle`)
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
runtimeOnly   'io.jsonwebtoken:jjwt-impl:0.13.0'
runtimeOnly   'io.jsonwebtoken:jjwt-jackson:0.13.0'
```
(spring-security는 추가하지 않음)

### Step 2 — 설정 중앙화 (`config-repo/gateway.yml`)
- `jwt.secret` 추가 → auth-service.yml과 **동일 값** (팀원과 합의)
- `@ConfigurationProperties` 또는 `@Value`로 secret 주입

### Step 3 — JWT 검증 유틸 (`com.lcs.gateway.jwt.JwtTokenValidator`)
- 모놀리식 `JwtTokenProvider`의 `init()`(키 생성), `parseClaims`, `validateToken`, `resolveToken` 이식
- 발급 로직 제외
- 예외: 만료/무효 구분 (모놀리식 `ExpiredJwtCustomException`/`InvalidJwtCustomException` 참고)

### Step 4 — GlobalFilter (`com.lcs.gateway.filter.JwtAuthenticationFilter`)
- `implements GlobalFilter, Ordered` (`getOrder()`는 라우팅보다 앞, 음수)
- 공개 경로 화이트리스트 통과
- 토큰 검증 → 실패 시 401 JSON(`{"message":"인증이 필요합니다."}`) + `setComplete()`
- 성공 시 `request.mutate().header("X-User-Id",...).header("X-Role",...)` 주입
- 클레임 형식은 모놀리식 그대로 (`userId`:Long, `roles`:콤마 구분 `ROLE_*` 문자열 → `X-Role`로 그대로 전달)

### Step 5 — 헤더 스푸핑 방어
- 클라이언트가 보낸 `X-User-Id`/`X-Role`을 필터에서 **먼저 제거** 후 게이트웨이가 재설정 (외부 위조 차단 — 신뢰 경계의 핵심)

### Step 6 — CORS 설정 (`com.lcs.gateway.config.CorsConfig`)
- 모놀리식 CORS 값(origin `localhost:3000`, 허용 헤더/메서드, `New-Access-Token` expose)을 리액티브 `CorsWebFilter`로 이식

### Step 7 — 테스트
- `JwtTokenValidator` 단위 테스트 (유효/만료/서명오류 토큰)
- GlobalFilter 테스트: 토큰 없음→401, 유효→헤더 주입 확인, 위조 헤더 제거 확인
- 게이트웨이 경유 통합 확인 (auth-service `/api/auth/ping` 통과, 보호 경로 401)

---

## 3. 팀 합의 필요 항목 (블로킹 의존성)

| 항목 | 상대 | 내용 |
|---|---|---|
| JWT secret | auth-service 담당 | config-repo에서 동일 값 공유 |
| 클레임 구조 | auth-service 담당 | `userId`, `roles`(콤마 문자열) 키 이름·형식 |
| 헤더 계약 | 전 서비스 | `X-User-Id`(확정, 필수) + `X-Role`(콤마 구분 `ROLE_*`, 선택) — subscription·course-service 실측 기준. 다운스트림이 이 헤더로 신원·롤 수신 |
| 경로 정합성 | member 담당 | 모놀리식 `/api/member/**`(단수) vs 게이트웨이 `/api/members/**`(복수) 조율 |

---

## 4. 위험 요소

- **리액티브 학습 곡선** — `Mono<Void>` 반환·`exchange.mutate()` 패턴 숙지 필요
- **secret 불일치** — 가장 흔한 실패. 검증 안 되면 secret부터 확인
- **필터 순서** — 검증이 라우팅보다 먼저여야 함 (`getOrder()` 음수)
- **헤더 위조** — Step 5 누락 시 신뢰 경계 붕괴

---

## 5. 산출물 파일 (예정)

```text
gateway/
├─ build.gradle                                    (수정: jjwt 추가)
└─ src/main/java/com/lcs/gateway/
   ├─ jwt/JwtTokenValidator.java                   (신규)
   ├─ filter/JwtAuthenticationFilter.java          (신규, GlobalFilter)
   └─ config/CorsConfig.java                       (신규)
config-repo/gateway.yml                            (수정: jwt.secret)
```
