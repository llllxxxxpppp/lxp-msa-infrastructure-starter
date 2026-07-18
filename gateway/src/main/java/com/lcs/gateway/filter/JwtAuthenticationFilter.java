package com.lcs.gateway.filter;

import com.lcs.gateway.jwt.JwtTokenValidator;
import com.lcs.gateway.jwt.exception.ExpiredTokenException;
import com.lcs.gateway.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 *   <li><b>인증만</b> 수행한다. 경로별 ROLE/소유권 인가는 각 도메인 서비스가 담당한다.</li>
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

    /**
     * 검증 없이 통과시키는 공개 경로
     */
    private static final List<String> PUBLIC_PATH_PATTERNS = List.of(
            "/api/auth/**",
            "/actuator/**");

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

    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(ROLE_HEADER);
                })
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(
                new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        DataBuffer buffer = response.bufferFactory()
                .wrap(UNAUTHORIZED_BODY.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 라우팅/로드밸런서 필터(≥10000)보다 먼저 실행되어야 인증 실패 요청이 다운스트림으로 넘어가지 않는다.
        return -1;
    }
}
