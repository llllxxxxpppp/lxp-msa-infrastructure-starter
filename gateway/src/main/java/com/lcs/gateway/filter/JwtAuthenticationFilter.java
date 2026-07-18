package com.lcs.gateway.filter;

import com.lcs.gateway.jwt.JwtTokenValidator;
import com.lcs.gateway.jwt.exception.ExpiredTokenException;
import com.lcs.gateway.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 모든 요청에 대해 JWT를 무상태로 검증하고, 검증한 신원을 다운스트림에 헤더로 전달하는
 * Spring Cloud Gateway 전역 필터
 *
 * <p>모놀리식 {@code JwtAuthenticationFilter}(서블릿 {@code OncePerRequestFilter})를 리액티브
 * {@code GlobalFilter}로 재작성했다. 다음 원칙을 따른다.
 * <ul>
 *   <li><b>인증 + 경로별 role 게이팅</b>을 수행한다. role만으로 판정되는 안정적인 경로 게이팅만 담당하고,
 *       소유권·상태처럼 서비스 로직과 함께 바뀌는 세밀 인가는 각 도메인 서비스가 담당한다.</li>
 *   <li>만료 토큰은 재발급을 시도하지 않고 401로 응답한다(재발급은 auth-service 책임).</li>
 *   <li>검증 성공 시 {@code X-User-Id}·{@code X-Role} 헤더를 주입한다. 위조 방지를 위해
 *       클라이언트가 보낸 동일 헤더는 먼저 제거한 뒤 Gateway가 값을 설정한다(신뢰 경계).</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    /**
     * 다운스트림이 신원을 수신하는 헤더
     */
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLE_HEADER = "X-Role";

    private static final String UNAUTHORIZED_BODY = "{\"message\":\"인증이 필요합니다.\"}";
    private static final String FORBIDDEN_BODY = "{\"message\":\"접근 권한이 없습니다.\"}";

    /**
     * 검증 없이 통과시키는 공개 경로
     */
    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            "/api/auth/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html");

    /**
     * 요청 경로가 규칙에 매칭되면 토큰 role에 허용하는 role이 하나라도 있어야 통과한다(없으면 403)
     * role 계층은 두지 않고 명시된 롤만 검증한다.
     * 소유권·상태 같은 세밀 인가는 여기에 두지 않고 각 서비스가 담당한다.
     */
    private static final List<RoleRule> ROLE_RULES = List.of(
            new RoleRule("/api/admin/**", Set.of("ROLE_ADMIN")),
            new RoleRule("/api/members/**", Set.of("ROLE_MEMBER")));

    private record RoleRule(String pathPattern, Set<String> allowedRoles) {
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtTokenValidator tokenValidator;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublicPath(request.getPath().value())) {
            // 공개 경로라도 클라이언트가 심은 신원 헤더는 제거해 다운스트림 위조를 차단한다.
            return chain.filter(exchange.mutate().request(stripIdentityHeaders(request)).build());
        }

        String token = tokenValidator.resolveToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return unauthorized(exchange);
        }

        Claims claims;
        try {
            claims = tokenValidator.parseClaims(token);
        } catch (ExpiredTokenException | InvalidTokenException e) {
            return unauthorized(exchange);
        }

        Long userId = tokenValidator.getUserId(claims);
        if (userId == null) {
            // 서명은 유효하나 userId가 없는 토큰은 신원을 특정할 수 없어 인증 실패로 처리한다.
            return unauthorized(exchange);
        }
        String roles = tokenValidator.getRoles(claims);

        if (!isAuthorized(request.getPath().value(), parseRoles(roles))) {
            return forbidden(exchange);
        }

        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(ROLE_HEADER);
                    headers.set(USER_ID_HEADER, String.valueOf(userId));
                    headers.set(ROLE_HEADER, roles);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 경로에 매칭되는 role 게이팅 규칙을 모두 만족하는지 검사한다. 매칭되는 규칙이 없으면
     * 게이트웨이 인가 대상이 아니므로 통과(true)시키고, 세밀 인가는 서비스에 위임한다.
     */
    private boolean isAuthorized(String path, Set<String> userRoles) {
        for (RoleRule rule : ROLE_RULES) {
            if (pathMatcher.match(rule.pathPattern(), path)
                    && rule.allowedRoles().stream().noneMatch(userRoles::contains)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toSet());
    }

    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(ROLE_HEADER);
                })
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        return writeJson(exchange, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        return writeJson(exchange, HttpStatus.FORBIDDEN, FORBIDDEN_BODY);
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(
                new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 라우팅/로드밸런서 필터(≥10000)보다 먼저 실행되어야 인증 실패 요청이 다운스트림으로 넘어가지 않는다.
        return -1;
    }
}
