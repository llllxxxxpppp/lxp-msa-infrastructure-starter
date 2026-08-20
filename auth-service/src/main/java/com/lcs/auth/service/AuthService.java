package com.lcs.auth.service;

import com.lcs.auth.controller.dto.response.TokenResponseDTO;
import com.lcs.auth.domain.RefreshToken;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public TokenResponseDTO login(String email, String password) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        email,
                        password));

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshTokenValue = jwtTokenProvider.createRefreshToken();

        refreshTokenRepository.findByEmail(authentication.getName())
                .ifPresent(refreshTokenRepository::delete);

        RefreshToken newRefreshToken = new RefreshToken(
                authentication.getName(),
                newRefreshTokenValue,
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenValidityMilliseconds()));

        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponseDTO(accessToken, newRefreshTokenValue);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
    }
}
