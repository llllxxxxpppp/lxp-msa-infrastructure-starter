package com.lcs.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lcs.auth.domain.RefreshToken;
import com.lcs.auth.exception.ExpiredJwtCustomException;
import com.lcs.auth.exception.InvalidJwtCustomException;
import com.lcs.auth.exception.InvalidRefreshTokenException;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.principal.CustomUserPrincipal;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshService 단위 테스트")
class RefreshServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private RefreshService refreshService;

    @Test
    @DisplayName("유효한 refresh token으로 요청하면 새 액세스 토큰을 발급한다")
    void refreshAccessToken_success_returnsNewAccessToken() {
        String oldToken = "old-refresh-token";
        given(jwtTokenProvider.validateToken(oldToken)).willReturn(true);

        RefreshToken entity = new RefreshToken("user@test.com", oldToken, Instant.now().plusSeconds(600));
        given(refreshTokenRepository.findByToken(oldToken)).willReturn(Optional.of(entity));

        UserDetails userDetails = new CustomUserPrincipal(
                1L, "user@test.com", "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), false);
        given(userDetailsService.loadUserByUsername("user@test.com")).willReturn(userDetails);
        given(jwtTokenProvider.createAccessToken(any())).willReturn("new-access-token");

        String result = refreshService.refreshAccessToken(oldToken);

        assertThat(result).isEqualTo("new-access-token");
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("JWT 자체가 만료되었으면 DB의 refresh token을 삭제하고 예외를 던진다")
    void refreshAccessToken_expiredJwt_deletesDbEntryAndThrows() {
        String oldToken = "expired-refresh-token";
        given(jwtTokenProvider.validateToken(oldToken))
                .willThrow(new ExpiredJwtCustomException("expired"));

        RefreshToken entity = new RefreshToken("user@test.com", oldToken, Instant.now().minusSeconds(1));
        given(refreshTokenRepository.findByToken(oldToken)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> refreshService.refreshAccessToken(oldToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).delete(entity);
    }

    @Test
    @DisplayName("서명이 유효하지 않은 토큰이면 DB 조회 없이 바로 예외를 던진다")
    void refreshAccessToken_invalidJwtSignature_throwsWithoutDbLookup() {
        String badToken = "tampered-refresh-token";
        given(jwtTokenProvider.validateToken(badToken))
                .willThrow(new InvalidJwtCustomException("invalid"));

        assertThatThrownBy(() -> refreshService.refreshAccessToken(badToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).findByToken(any());
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("JWT는 유효하지만 DB에 해당 토큰이 없으면 예외를 던진다")
    void refreshAccessToken_notFoundInDb_throws() {
        String token = "valid-jwt-but-missing-in-db";
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(refreshTokenRepository.findByToken(token)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshService.refreshAccessToken(token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("DB에 저장된 만료 시각이 지났으면 토큰을 삭제하고 예외를 던진다")
    void refreshAccessToken_dbEntryExpired_deletesAndThrows() {
        String token = "token-expired-in-db";
        given(jwtTokenProvider.validateToken(token)).willReturn(true);

        RefreshToken entity = new RefreshToken("user@test.com", token, Instant.now().minusSeconds(1));
        given(refreshTokenRepository.findByToken(token)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> refreshService.refreshAccessToken(token))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).delete(entity);
    }

    @Test
    @DisplayName("탈퇴/정지된 회원이면 refresh token을 삭제하고 예외를 던진다")
    void refreshAccessToken_disabledMember_deletesAndThrows() {
        String token = "valid-jwt-but-disabled-member";
        given(jwtTokenProvider.validateToken(token)).willReturn(true);

        RefreshToken entity = new RefreshToken("user@test.com", token, Instant.now().plusSeconds(600));
        given(refreshTokenRepository.findByToken(token)).willReturn(Optional.of(entity));

        UserDetails disabledUserDetails = new CustomUserPrincipal(
                1L, "user@test.com", "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), true);
        given(userDetailsService.loadUserByUsername("user@test.com")).willReturn(disabledUserDetails);

        assertThatThrownBy(() -> refreshService.refreshAccessToken(token))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("탈퇴/정지된 회원");

        verify(refreshTokenRepository).delete(entity);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }
}
