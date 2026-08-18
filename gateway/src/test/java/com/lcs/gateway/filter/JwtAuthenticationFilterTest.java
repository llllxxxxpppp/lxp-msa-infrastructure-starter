package com.lcs.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.lcs.gateway.jwt.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "4d553a82c87c2a2e0b7000d63eb926f3ef75fd528977b9c956efcec692845953";
    private static final String OTHER_SECRET =
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-Role";

    private JwtAuthenticationFilter filter;
    private SecretKey key;

    /** chain으로 넘어온 exchange를 캡처해 다운스트림 전달 여부·헤더를 검사한다. */
    private static final class CapturingChain implements GatewayFilterChain {
        private ServerWebExchange captured;
        private boolean called;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.captured = exchange;
            this.called = true;
            return Mono.empty();
        }
    }

    @BeforeEach
    void setUp() {
        JwtTokenValidator validator = new JwtTokenValidator();
        ReflectionTestUtils.setField(validator, "secretKey", SECRET);
        validator.init();
        filter = new JwtAuthenticationFilter(validator);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String token(SecretKey signingKey, Long userId, String roles, long expiryOffsetMillis) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject("user@example.com")
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryOffsetMillis))
                .signWith(signingKey, Jwts.SIG.HS512);
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.compact();
    }

    @Test
    void 보호_경로에_토큰이_없으면_401이고_다운스트림으로_넘기지_않는다() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses"));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.called).isFalse();
    }

    @Test
    void 유효한_토큰이면_신원_헤더를_주입해_다운스트림으로_넘긴다() {
        String jwt = token(key, 42L, "ROLE_ADMIN,ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
        var forwarded = chain.captured.getRequest().getHeaders();
        assertThat(forwarded.getFirst(USER_ID_HEADER)).isEqualTo("42");
        assertThat(forwarded.getFirst(ROLE_HEADER)).isEqualTo("ROLE_ADMIN,ROLE_MEMBER");
    }

    @Test
    void 만료된_토큰이면_401이다() {
        String jwt = token(key, 1L, "ROLE_MEMBER", -1_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.called).isFalse();
    }

    @Test
    void 다른_키로_서명된_토큰이면_401이다() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String jwt = token(wrongKey, 1L, "ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.called).isFalse();
    }

    @Test
    void userId가_없는_토큰이면_401이다() {
        String jwt = token(key, null, "ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.called).isFalse();
    }

    @Test
    void 공개_경로는_토큰_없이_통과한다() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/ping"));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void signup_경로는_토큰_없이_통과한다() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/members/signup"));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/v3/api-docs",
            "/v3/api-docs/course-service",
            "/swagger-ui/index.html",
            "/swagger-ui.html"})
    void swagger_문서_경로는_토큰_없이_통과한다(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void 클라이언트가_위조한_신원_헤더는_검증값으로_덮어쓴다() {
        String jwt = token(key, 42L, "ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses")
                        .header("Authorization", "Bearer " + jwt)
                        .header(USER_ID_HEADER, "999")          // 위조 시도
                        .header(ROLE_HEADER, "ROLE_ADMIN"));     // 위조 시도
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        var forwarded = chain.captured.getRequest().getHeaders();
        assertThat(forwarded.get(USER_ID_HEADER)).containsExactly("42");
        assertThat(forwarded.get(ROLE_HEADER)).containsExactly("ROLE_MEMBER");
    }

    @Test
    void 공개_경로에_위조_신원_헤더가_있으면_제거한다() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/ping")
                        .header(USER_ID_HEADER, "999")
                        .header(ROLE_HEADER, "ROLE_ADMIN"));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        var forwarded = chain.captured.getRequest().getHeaders();
        assertThat(forwarded.get(USER_ID_HEADER)).isNull();
        assertThat(forwarded.get(ROLE_HEADER)).isNull();
    }

    @Test
    void 보호_경로에_토큰없이_위조_헤더만_보내면_401이다() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses")
                        .header(USER_ID_HEADER, "999")
                        .header(ROLE_HEADER, "ROLE_ADMIN"));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(chain.called).isFalse();
    }

    // --- 경로별 role 게이팅 ---

    @Test
    void admin_경로는_ADMIN_롤이면_통과한다() {
        String jwt = token(key, 1L, "ROLE_ADMIN", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/members").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
    }

    @Test
    void admin_경로는_ADMIN_롤이_없으면_403이다() {
        String jwt = token(key, 1L, "ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/members").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chain.called).isFalse();
    }

    @Test
    void members_경로는_MEMBER_롤이면_통과한다() {
        String jwt = token(key, 1L, "ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/members/ping").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
    }

    @Test
    void members_경로는_INSTRUCTOR나_ADMIN도_통과한다() {
        // /api/members/** 하위는 현재 자기 자신(/me) 엔드포인트뿐이므로,
        // 단일 role을 갖는 강사·관리자도 자기 계정에 접근할 수 있어야 한다.
        for (String role : new String[] {"ROLE_INSTRUCTOR", "ROLE_ADMIN"}) {
            String jwt = token(key, 1L, role, 60_000);
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/members/ping").header("Authorization", "Bearer " + jwt));
            CapturingChain chain = new CapturingChain();

            filter.filter(exchange, chain).block();

            assertThat(chain.called).as("role=%s", role).isTrue();
        }
    }

    @Test
    void 규칙이_없는_경로는_롤과_무관하게_통과한다() {
        // /api/courses는 role 게이팅 규칙 대상이 아니므로 인증만 통과하면 된다(세밀 인가는 서비스 몫).
        String jwt = token(key, 1L, "ROLE_MEMBER", 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/courses").header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertThat(chain.called).isTrue();
    }

    // --- course 경로별 role 게이팅 (모놀리식 SecurityConfig 이식) ---

    @ParameterizedTest(name = "[{index}] {0} {1} role={2} forbidden={3}")
    @CsvSource({
            // [추가] 담당 강좌 목록
            "GET,    /api/courses/instructor/me,            ROLE_INSTRUCTOR, false",
            "GET,    /api/courses/instructor/me,            ROLE_MEMBER,     true",
            // INSTRUCTOR 전용
            "POST,   /api/courses,                       ROLE_INSTRUCTOR, false",
            "POST,   /api/courses,                       ROLE_MEMBER,     true",
            "POST,   /api/courses,                       ROLE_ADMIN,      true",
            "POST,   /api/courses/5/publish,             ROLE_INSTRUCTOR, false",
            "POST,   /api/courses/5/publish,             ROLE_ADMIN,      true",
            "POST,   /api/courses/5/lectures,            ROLE_INSTRUCTOR, false",
            "POST,   /api/courses/5/missions,            ROLE_MEMBER,     true",
            // INSTRUCTOR 또는 ADMIN
            "POST,   /api/courses/5/unpublish,           ROLE_ADMIN,      false",
            "POST,   /api/courses/5/unpublish,           ROLE_INSTRUCTOR, false",
            "POST,   /api/courses/5/unpublish,           ROLE_MEMBER,     true",
            "DELETE, /api/courses/5,                     ROLE_ADMIN,      false",
            "DELETE, /api/courses/5,                     ROLE_MEMBER,     true",
            "PATCH,  /api/courses/5/reorder,             ROLE_INSTRUCTOR, false",
            "DELETE, /api/courses/5/lectures/3,          ROLE_ADMIN,      false",
            "DELETE, /api/courses/5/missions/3,          ROLE_MEMBER,     true",
            // GET 등 규칙 없는 메서드/경로는 role 무관 통과
            "GET,    /api/courses,                       ROLE_MEMBER,     false",
            "GET,    /api/courses/5,                     ROLE_MEMBER,     false",

            // [추가]
            "GET,    /api/ai/courses/1/documents,        ROLE_INSTRUCTOR, false",
            "GET,    /api/ai/courses/1/documents,        ROLE_MEMBER,     true",
            "POST,   /api/ai/courses/1/documents,        ROLE_INSTRUCTOR, false",
            "POST,   /api/ai/courses/1/documents,        ROLE_MEMBER,     true",
            "DELETE, /api/ai/courses/1/documents/doc-1,  ROLE_INSTRUCTOR, false",
            "DELETE, /api/ai/courses/1/documents/doc-1,  ROLE_MEMBER,     true",
            "POST,   /api/ai/courses/1/chat,             ROLE_MEMBER,     false"})
    void course_경로_role_게이팅(String method, String path, String role, boolean forbidden) {
        String jwt = token(key, 1L, role, 60_000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.valueOf(method), path)
                        .header("Authorization", "Bearer " + jwt));
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        if (forbidden) {
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(chain.called).isFalse();
        } else {
            assertThat(chain.called).isTrue();
        }
    }

    @Test
    void 필터_순서는_라우팅보다_앞선다() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
