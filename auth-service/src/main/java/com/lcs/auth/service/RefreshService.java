package com.lcs.auth.service;

import com.lcs.auth.exception.ExpiredJwtCustomException;
import com.lcs.auth.exception.InvalidJwtCustomException;
import com.lcs.auth.exception.InvalidRefreshTokenException;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.repository.RefreshTokenRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class RefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService userDetailsService;

    public RefreshService(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailsService = userDetailsService;
    }

    public String refreshAccessToken(String oldRefreshTokenValue) {
        try {
            jwtTokenProvider.validateToken(oldRefreshTokenValue);
        } catch (ExpiredJwtCustomException e) {
            // [수정] 만료된 Refresh Token을 Redis에서 바로 삭제
            refreshTokenRepository.delete(oldRefreshTokenValue);
            throw new InvalidRefreshTokenException(e.getMessage(), e);
        } catch (InvalidJwtCustomException e) {
            throw new InvalidRefreshTokenException(e.getMessage(), e);
        }

        // [수정]
        // Redis
        // Key = Refresh Token
        // Value = email
        String email = refreshTokenRepository.findEmailByToken(oldRefreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Refresh token not found in Redis"));

        // Redis에서 조회한 email로 기존 회원 정보 조회
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!userDetails.isEnabled()) {

            // [유지] 정지/탈퇴 회원이면 Redis의 Refresh Token 삭제
            refreshTokenRepository.delete(oldRefreshTokenValue);

            throw new InvalidRefreshTokenException(
                    "탈퇴/정지된 회원은 토큰을 재발급할 수 없습니다.");
        }

        // [유지] 기존 Authentication 생성
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities());

        // [유지] 기존 방식 그대로 새로운 Access Token 발급
        return jwtTokenProvider.createAccessToken(authentication);
    }
}