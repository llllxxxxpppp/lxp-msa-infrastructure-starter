package com.lcs.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lcs.auth.controller.dto.response.TokenResponseDTO;
import com.lcs.auth.domain.RefreshToken;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    @DisplayName("로그인 성공 시 기존 refresh token을 삭제하고 새 토큰을 발급/저장한다")
    void login_success_deletesExistingRefreshTokenAndSavesNew() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("user@test.com", null, List.of());
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenValidityMilliseconds()).willReturn(60_000L);

        RefreshToken existing = new RefreshToken("user@test.com", "old-refresh-token", Instant.now());
        given(refreshTokenRepository.findByEmail("user@test.com")).willReturn(Optional.of(existing));

        TokenResponseDTO result = authService.login("user@test.com", "password123");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).delete(existing);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("기존 refresh token이 없으면 삭제 없이 새 토큰만 저장한다")
    void login_noExistingRefreshToken_doesNotDeleteAnything() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("user@test.com", null, List.of());
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.createAccessToken(authentication)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken()).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenValidityMilliseconds()).willReturn(60_000L);
        given(refreshTokenRepository.findByEmail("user@test.com")).willReturn(Optional.empty());

        authService.login("user@test.com", "password123");

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
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
    @DisplayName("존재하는 refresh token으로 로그아웃하면 해당 토큰을 삭제한다")
    void logout_existingToken_deletesIt() {
        RefreshToken token = new RefreshToken("user@test.com", "refresh-token", Instant.now().plusSeconds(60));
        given(refreshTokenRepository.findByToken("refresh-token")).willReturn(Optional.of(token));

        authService.logout("refresh-token");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("존재하지 않는 refresh token으로 로그아웃하면 아무 일도 일어나지 않는다")
    void logout_unknownToken_doesNothing() {
        given(refreshTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        authService.logout("unknown-token");

        verify(refreshTokenRepository, never()).delete(any());
    }
}
