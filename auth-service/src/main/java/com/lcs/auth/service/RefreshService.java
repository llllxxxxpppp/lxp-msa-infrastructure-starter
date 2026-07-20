package com.lcs.auth.service;

import com.lcs.auth.domain.RefreshToken;
import com.lcs.auth.exception.ExpiredJwtCustomException;
import com.lcs.auth.exception.InvalidJwtCustomException;
import com.lcs.auth.exception.InvalidRefreshTokenException;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.repository.RefreshTokenRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService userDetailsService;

    public RefreshService(
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public String refreshAccessToken(String oldRefreshTokenValue) {
        try {
            jwtTokenProvider.validateToken(oldRefreshTokenValue);
        } catch (ExpiredJwtCustomException e) {
            refreshTokenRepository.findByToken(oldRefreshTokenValue)
                    .ifPresent(refreshTokenRepository::delete);
            throw new InvalidRefreshTokenException(e.getMessage(), e);
        } catch (InvalidJwtCustomException e) {
            throw new InvalidRefreshTokenException(e.getMessage(), e);
        }

        RefreshToken oldRefreshTokenEntity =
                refreshTokenRepository.findByToken(oldRefreshTokenValue)
                        .orElseThrow(() -> new InvalidRefreshTokenException(
                                "Refresh token not found in database"));

        if (oldRefreshTokenEntity.isExpired()) {
            refreshTokenRepository.delete(oldRefreshTokenEntity);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(oldRefreshTokenEntity.getEmail());

        if (!userDetails.isEnabled()) {
            refreshTokenRepository.delete(oldRefreshTokenEntity);
            throw new InvalidRefreshTokenException("탈퇴/정지된 회원은 토큰을 재발급할 수 없습니다.");
        }

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities());

        return jwtTokenProvider.createAccessToken(authentication);
    }
}
