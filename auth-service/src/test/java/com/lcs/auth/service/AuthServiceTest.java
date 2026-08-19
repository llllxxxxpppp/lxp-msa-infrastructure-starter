package com.lcs.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lcs.auth.controller.dto.response.TokenResponseDTO;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.repository.RefreshTokenRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공 시 Access/Refresh Token을 발급하고 Redis에 Refresh Token→email을 TTL과 함께 저장한다")
    void login_success_issuesTokensAndSavesRefreshTokenInRedis() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("user@test.com", null, List.of());
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenValidityMilliseconds()).willReturn(60_000L);

        TokenResponseDTO result = authService.login("user@test.com", "password123");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save("refresh-token", "user@test.com", 60L);
    }

    @Test
    @DisplayName("자격증명이 잘못되면 예외가 그대로 전파되고 refresh token 관련 작업은 수행되지 않는다")
    void login_badCredentials_propagatesExceptionWithoutTouchingRefreshTokens() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("user@test.com", "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("로그아웃하면 Redis에서 해당 Refresh Token Key를 삭제한다")
    void logout_deletesRefreshTokenFromRedis() {
        authService.logout("refresh-token");

        verify(refreshTokenRepository).delete("refresh-token");
    }
}
