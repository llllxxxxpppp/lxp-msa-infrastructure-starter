package com.lcs.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Gateway 단일 진입점에서 CORS를 중앙 처리한다. 모놀리식 {@code SecurityConfig}의 CORS 설정을
 * 리액티브 {@link CorsWebFilter}로 이식했다.
 *
 * <p>{@code CorsWebFilter}는 {@code WebFilter}라 게이트웨이의 {@code GlobalFilter}(JWT 인증)보다
 * 먼저 실행된다. 따라서 {@code Authorization} 헤더가 없는 프리플라이트({@code OPTIONS})는 여기서
 * 처리·응답되어 JWT 필터에 도달하지 않는다.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Refresh-Token"));
        configuration.setExposedHeaders(List.of("Authorization", "New-Access-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}
