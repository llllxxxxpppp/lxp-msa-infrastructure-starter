package com.lcs.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Consul health check(/actuator/health)와 인증 관련 공개 API가
 * SecurityFilterChain에 의해 차단되지 않는지 확인하는 회귀 테스트.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.cloud.consul.enabled=false"
})
@AutoConfigureMockMvc
@DisplayName("SecurityConfig 회귀 테스트 (actuator/공개 API 접근 가능 여부)")
class SecurityConfigActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("actuator/health는 인증 없이 접근 가능하다 (Consul 헬스체크)")
    void actuatorHealth_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("api/auth/ping은 인증 없이 접근 가능하다")
    void authPing_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("permitAll 목록에 없는 임의 경로는 인증되지 않으면 차단된다")
    void unmappedProtectedPath_isRejected() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().is4xxClientError());
    }
}
