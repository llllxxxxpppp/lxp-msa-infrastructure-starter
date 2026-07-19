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
  - 근거: 게이트웨이가 하는 인가는 role 게이팅 수준(경로↔허용 role)이라 규칙 수가 적어 GlobalFilter 분기로 충분. 소유권 등 세밀 인가는 서비스가 담당하므로 Spring Security의 선언적 인가 이점이 크지 않고, 리액티브 시큐리티의 무게·학습비용 대비 이득이 작음.
- **만료 토큰 처리**: Gateway는 401만 반환. 재발급(refresh)은 auth-service가 담당 (무상태 게이트웨이 유지).
- **인가 위치**: Gateway는 **인증 + 경로별 role 게이팅**(모놀리식 `SecurityConfig` 규칙 이식). role 차원만 거르고, 소유권(작성자 본인)·정지 상태 같은 세밀 인가는 각 도메인 서비스가 소유.

### 왜 그대로 이식이 안 되는가
- 모놀리식은 **서블릿(Spring MVC / Tomcat)** 기반: `OncePerRequestFilter`, `HttpServletRequest`, `filterChain.doFilter()`(void).
- 스타터 gateway는 **리액티브(Spring WebFlux / Netty)** 기반: `GlobalFilter`, `ServerWebExchange`, `Mono<Void>` 반환.
- 근거: `gateway/build.gradle` → `spring-cloud-starter-gateway-server-webflux` (스타터가 WebFlux 변형을 채택. 서블릿 기반 Gateway MVC 변형도 있으나 아래 근거로 WebFlux 유지).
- 따라서 **필터 골격은 재작성**, 그 안의 **JWT 검증 로직(jjwt 호출)은 재사용**.

### WebFlux vs MVC 선택 근거

Spring Cloud Gateway는 두 런타임 변형이 있다.

| | **Gateway Server WebFlux** (채택) | **Gateway Server MVC** |
|---|---|---|
| 아티팩트 | `spring-cloud-starter-gateway-server-webflux` | `spring-cloud-starter-gateway-server-webmvc` |
| 기반 | Spring WebFlux + Netty (Reactor) | Spring MVC + 서블릿(Tomcat) |
| I/O 모델 | 논블로킹, 이벤트 루프 (소수 스레드로 대량 동시 연결) | 블로킹, 요청당 스레드 |
| 필터 API | `GlobalFilter`/`GatewayFilter`, `ServerWebExchange`, `Mono` | 함수형 라우팅 + `HandlerFilterFunction` (서블릿/명령형) |
| 성숙도 | 원조·주류, 문서·예제 풍부 | 비교적 신규 |

**트레이드오프**: 작성 편의성(MVC) ↔ 게이트웨이 역할 적합성 + 생태계 성숙도(WebFlux).
- MVC였다면 서블릿 기반 모놀리식 필터를 거의 그대로 이식해 **구현이 더 쉬웠을 것**(리액티브 마찰 없음). 대신 논블로킹 이점·주류 생태계를 포기.
- WebFlux는 리액티브 학습 곡선이 있고, 필터 내 블로킹 호출(JDBC 등)이 금기.

**우리 상황에서 WebFlux를 택한 이유**
1. 스타터가 이미 `-server-webflux`를 채택 → 공통 인프라·스택과 일관.
2. 게이트웨이는 I/O 바운드 프록시 → 논블로킹이 본래 강점이 발휘되는 영역(향후 트래픽·서비스 증가 대비).
3. **우리 필터는 순수 CPU 작업(jjwt 검증)이라 WebFlux의 최대 비용(블로킹 금지)을 안 치름** — 블로킹이 필요한 refresh 재발급(DB)·정지 상태 조회는 auth-service·도메인 서비스로 위임했기 때문.
4. 필터 로직이 단순(검증 → 헤더 주입 → 라우팅)해 리액티브 조합의 복잡함까지 가지 않아 학습 곡선이 감수 가능.

**결론**: 게이트웨이 성격(I/O 바운드)·스타터 스택·생태계 성숙도상 WebFlux가 적합. 우리 필터는 블로킹 I/O가 없어 WebFlux의 핵심 제약이 부담이 되지 않는다. (단순함·이식 편의가 최우선이고 확장성 요구가 낮았다면 MVC도 합리적 대안이었다.)

---

## 0. 스코프

**Gateway가 하는 일**
- 모든 요청에서 JWT 서명·만료 검증 (무상태)
- 경로별 role 게이팅(모놀리식 `SecurityConfig` 규칙): 매칭 규칙의 role 불충족 시 403
- 검증·인가 통과 → `X-User-Id`, `X-Role` 헤더 주입 후 다운스트림 전달
- 토큰 없음/만료/서명오류/userId 부재 → 401
- 공개 경로(`/api/auth/**`, `/actuator/**`, Swagger 문서 경로)는 검증 없이 통과
- CORS 중앙 처리

**Gateway가 안 하는 일**
- 토큰 발급·리프레시·로그인 → auth-service
- 소유권·정지 상태 등 데이터 조회가 필요한 세밀 인가 → 각 도메인 서비스

---

## 1. 모놀리식 자산 분류

| 모놀리식 파일 | Gateway 처리 |
|---|---|
| `JwtTokenProvider` (`validateToken`, `parseClaims`, `resolveToken`, 키 초기화) | ✅ 검증 부분만 이식 |
| `JwtTokenProvider` (`createAccessToken/RefreshToken`, refresh validity) | ❌ auth-service로 |
| `JwtAuthenticationFilter` (OncePerRequestFilter) | ♻️ GlobalFilter로 재작성 (로직 참고) |
| `CustomUserPrincipal` | ❌ Gateway 불필요 (다운스트림이 헤더로 재구성) |
| `SecurityConfig`의 CORS 설정 | ♻️ 리액티브 `CorsWebFilter`로 재작성 |
| `SecurityConfig`의 authz 규칙 | ♻️ role 규칙은 필터의 경로별 role 게이팅으로 이식 / 소유권·정지 등 세밀 인가는 각 서비스 |
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

### Step 2 — 설정 중앙화 (`config-repo/application.yml`)
- gateway·auth가 **동일 값**을 쓰도록 공통 `application.yml`에 `jwt.secret` 단일 출처로 배치 (`${JWT_SECRET:...}` 오버라이드 가능)
- `@Value("${jwt.secret}")`로 주입

### Step 3 — JWT 검증 유틸 (`com.lcs.gateway.jwt.JwtTokenValidator`)
- 모놀리식 `JwtTokenProvider`의 `init()`(키 생성), `parseClaims`, `validateToken`, `resolveToken` 이식
- 발급 로직 제외
- 예외: 만료/무효 구분 (모놀리식 `ExpiredJwtCustomException`/`InvalidJwtCustomException` 참고)

### Step 4 — GlobalFilter (`com.lcs.gateway.filter.JwtAuthenticationFilter`)
- `implements GlobalFilter, Ordered` (`getOrder()`는 라우팅보다 앞, 음수)
- 공개 경로 화이트리스트 통과
- 토큰 검증 실패(없음/만료/서명오류/userId 부재) → 401 JSON(`{"message":"인증이 필요합니다."}`)
- 경로별 role 게이팅: (HTTP 메서드, 경로 패턴)→허용 role 규칙 검사, 불충족 시 403 JSON(`{"message":"접근 권한이 없습니다."}`)
- 성공 시 `request.mutate().header("X-User-Id",...).header("X-Role",...)` 주입
- 클레임 형식은 모놀리식 그대로 (`userId`:Long, `roles`:콤마 구분 `ROLE_*` 문자열 → `X-Role`로 그대로 전달)
- role 규칙(모놀리식 `SecurityConfig` 이식): `/api/admin/**`→ADMIN, `/api/members/**`→MEMBER, course/lecture/mission의 메서드별 INSTRUCTOR·ADMIN. role 계층 없이 명시 role만 검증

### Step 5 — 헤더 스푸핑 방어
- 클라이언트가 보낸 `X-User-Id`/`X-Role`을 필터에서 **먼저 제거** 후 게이트웨이가 재설정 (외부 위조 차단 — 신뢰 경계의 핵심)

### Step 6 — CORS 설정 (`com.lcs.gateway.config.CorsConfig`)
- 모놀리식 CORS 값(origin `localhost:3000`, 허용 헤더/메서드)을 리액티브 `CorsWebFilter`로 이식
- `CorsWebFilter`(WebFilter)가 JWT GlobalFilter보다 먼저 실행돼 프리플라이트(OPTIONS)가 인증에 막히지 않음
- 참고: `New-Access-Token` expose는 게이트웨이가 인라인 재발급을 하지 않으므로 불필요(정리 대상). `X-Refresh-Token` allowedHeader는 클라이언트가 auth-service로 보낼 때 필요해 유지

### Step 7 — 테스트
- `JwtTokenValidatorTest`(8): 유효/만료/서명오류/형식오류, roles 부재, Bearer 파싱
- `JwtAuthenticationFilterTest`(36): 401(없음/만료/서명오류/userId부재), 헤더 주입, 스푸핑 방어, 공개·Swagger 경로 통과, 경로별 role 게이팅(401 vs 403 vs 통과)
- `CorsConfigTest`(3): 프리플라이트 허용/거부, 실요청
- Docker 스택 E2E(수동): 인증 게이팅·role 게이팅·라우팅·CORS 확인
- **합계: 단위/필터 47개 + E2E 통과**

---

## 3. 팀 합의 필요 항목 (블로킹 의존성)

| 항목 | 상대 | 내용 |
|---|---|---|
| JWT secret | auth-service 담당 | config-repo에서 동일 값 공유 |
| 클레임 구조 | auth-service 담당 | `userId`, `roles`(콤마 문자열) 키 이름·형식 |
| 헤더 계약 | 전 서비스 | `X-User-Id`(확정, 필수) + `X-Role`(콤마 구분 `ROLE_*`, 선택) — subscription·course-service 실측 기준. 다운스트림이 이 헤더로 신원·롤 수신 |
| 경로 정합성 | member 담당 | 모놀리식 `/api/member/**`(단수) vs 게이트웨이 `/api/members/**`(복수) 조율 |
| 재발급 흐름 | auth 담당 | 만료 시 게이트웨이 401 → 클라이언트가 `/api/auth/**`로 재발급 호출 → auth-service가 RefreshToken DB 확인 후 재발급 (모놀리식 `RefreshService`·`RefreshTokenRepository` 이식) |
| 재발급 트리거 위치 | front·auth·gateway | 401 후 refresh 호출·재시도 주체: (A)프론트 인터셉터[현재 전제·권장] / (B)게이트웨이 주도 / (C)BFF 중 결정 |
| 세밀 인가 유지 | course 담당 | 게이트웨이는 course role만 거름 → `checkOwnership`·`rejectIfSuspended`(소유권·정지)는 서비스가 계속 유지 |

---

## 4. 위험 요소

- **리액티브 학습 곡선** — `Mono<Void>` 반환·`exchange.mutate()` 패턴 숙지 필요
- **secret 불일치** — 가장 흔한 실패. 검증 안 되면 secret부터 확인
- **필터 순서** — 검증이 라우팅보다 먼저여야 함 (`getOrder()` 음수)
- **헤더 위조** — Step 5 누락 시 신뢰 경계 붕괴

---

## 5. 산출물 파일

```text
gateway/
├─ build.gradle                                    (수정: jjwt 추가)
└─ src/
   ├─ main/java/com/lcs/gateway/
   │  ├─ jwt/JwtTokenValidator.java                (신규)
   │  ├─ jwt/exception/ExpiredTokenException.java  (신규)
   │  ├─ jwt/exception/InvalidTokenException.java  (신규)
   │  ├─ filter/JwtAuthenticationFilter.java       (신규, GlobalFilter + role 게이팅)
   │  └─ config/CorsConfig.java                    (신규)
   └─ test/java/com/lcs/gateway/
      ├─ jwt/JwtTokenValidatorTest.java            (신규)
      ├─ filter/JwtAuthenticationFilterTest.java   (신규)
      └─ config/CorsConfigTest.java                (신규)
config-repo/application.yml                         (수정: 공유 jwt.secret)
```
