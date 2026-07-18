package com.lcs.auth.config;

import com.lcs.auth.handler.CustomAccessDeniedHandler;
import com.lcs.auth.handler.CustomAuthenticationEntryPoint;
import com.lcs.auth.jwt.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            CustomAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
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

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/api/auth/**").permitAll();
            auth.requestMatchers("/v3/api-docs/**").permitAll();
            auth.requestMatchers("/swagger-ui/**").permitAll();
            auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
            auth.requestMatchers("/api/member/**").hasRole("MEMBER");
            auth.requestMatchers(HttpMethod.POST, "/api/courses").hasRole("INSTRUCTOR");
            auth.requestMatchers(HttpMethod.POST, "/api/courses/{courseId}/publish")
                    .hasRole("INSTRUCTOR");
            auth.requestMatchers(HttpMethod.POST, "/api/courses/{courseId}/unpublish")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.requestMatchers(HttpMethod.DELETE, "/api/courses/{courseId}")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.requestMatchers(HttpMethod.PATCH, "/api/courses/{courseId}/reorder")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.requestMatchers(HttpMethod.POST, "/api/courses/{courseId}/lectures")
                    .hasRole("INSTRUCTOR");
            auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/courses/{courseId}/lectures/{lectureId}/publish").hasRole("INSTRUCTOR");
            auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/courses/{courseId}/lectures/{lectureId}/unpublish")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.requestMatchers(HttpMethod.DELETE, "/api/courses/{courseId}/lectures/{lectureId}")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.requestMatchers(HttpMethod.POST, "/api/courses/{courseId}/missions")
                    .hasRole("INSTRUCTOR");
            auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/courses/{courseId}/missions/{missionId}/publish").hasRole("INSTRUCTOR");
            auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/courses/{courseId}/missions/{missionId}/unpublish")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.requestMatchers(HttpMethod.DELETE, "/api/courses/{courseId}/missions/{missionId}")
                    .hasAnyRole("INSTRUCTOR", "ADMIN");
            auth.anyRequest().authenticated();
        });

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public WebSecurityCustomizer configureH2ConsoleEnable() {
        return web -> web.ignoring().requestMatchers(PathRequest.toH2Console());
    }
}
