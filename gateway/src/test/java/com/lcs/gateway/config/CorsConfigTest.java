package com.lcs.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    private CorsWebFilter corsWebFilter;

    @BeforeEach
    void setUp() {
        corsWebFilter = new CorsConfig().corsWebFilter();
    }

    @Test
    void 허용_오리진의_프리플라이트는_CORS_헤더를_붙이고_체인을_타지_않는다() {
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);
        WebFilterChain chain = exchange -> {
            downstreamCalled.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "http://localhost:8080/api/courses")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        corsWebFilter.filter(exchange, chain).block();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo(ALLOWED_ORIGIN);
        assertThat(responseHeaders.getAccessControlAllowMethods()).contains(HttpMethod.POST);
        // 프리플라이트는 여기서 종료 → 다운스트림(및 이후 JWT GlobalFilter)으로 넘어가지 않는다.
        assertThat(downstreamCalled).isFalse();
    }

    @Test
    void 허용되지_않은_오리진의_프리플라이트는_CORS_허용_헤더가_없다() {
        WebFilterChain chain = exchange -> Mono.empty();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "http://localhost:8080/api/courses")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        corsWebFilter.filter(exchange, chain).block();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void 허용_오리진의_실요청은_CORS_헤더를_붙이고_체인을_계속_탄다() {
        AtomicBoolean downstreamCalled = new AtomicBoolean(false);
        WebFilterChain chain = exchange -> {
            downstreamCalled.set(true);
            return Mono.empty();
        };
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost:8080/api/courses")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN));

        corsWebFilter.filter(exchange, chain).block();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertThat(responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo(ALLOWED_ORIGIN);
        // 실제 요청은 CORS 헤더만 추가되고 정상적으로 다운스트림으로 진행된다.
        assertThat(downstreamCalled).isTrue();
    }
}
