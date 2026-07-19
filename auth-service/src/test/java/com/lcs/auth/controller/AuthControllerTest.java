package com.lcs.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.auth.config.SecurityConfig;
import com.lcs.auth.controller.dto.request.LoginRequestDTO;
import com.lcs.auth.controller.dto.response.TokenResponseDTO;
import com.lcs.auth.service.AuthService;
import com.lcs.auth.service.RefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.h2.console.enabled=false")
@DisplayName("AuthController 웹 계층 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshService refreshService;

    @Test
    @DisplayName("ping은 서비스 상태 정보를 반환한다")
    void ping_returnsServiceStatus() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("auth-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("유효한 로그인 요청은 access/refresh 토큰을 반환한다")
    void login_validRequest_returnsTokens() throws Exception {
        given(authService.login("user@test.com", "password123"))
                .willReturn(new TokenResponseDTO("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO("user@test.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("요청 값 검증에 실패하면 400 Bad Request를 반환한다")
    void login_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO("not-an-email", "123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자격증명이 잘못되면 401 Unauthorized를 반환한다")
    void login_badCredentials_returns401() throws Exception {
        given(authService.login(anyString(), anyString()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO("user@test.com", "wrongpass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰과 함께 로그아웃하면 서비스가 호출되고 204를 반환한다")
    void logout_withToken_invokesServiceAndReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("X-Refresh-Token", "refresh-token"))
                .andExpect(status().isNoContent());

        verify(authService).logout("refresh-token");
    }

    @Test
    @DisplayName("토큰 없이 로그아웃해도 서비스 호출 없이 204를 반환한다")
    void logout_withoutToken_returns204WithoutInvokingService() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService, never()).logout(any());
    }

    @Test
    @DisplayName("X-Refresh-Token 헤더 없이 refresh를 요청하면 400을 반환한다")
    void refresh_withoutToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 refresh token으로 요청하면 새 액세스 토큰을 반환한다")
    void refresh_withToken_returnsNewAccessToken() throws Exception {
        given(refreshService.refreshAccessToken("old-refresh-token")).willReturn("new-access-token");

        mockMvc.perform(post("/api/auth/refresh").header("X-Refresh-Token", "old-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }
}
