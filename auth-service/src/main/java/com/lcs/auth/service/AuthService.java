package com.lcs.auth.service;

import com.lcs.auth.controller.dto.response.TokenResponseDTO;
import com.lcs.auth.jwt.JwtTokenProvider;
import com.lcs.auth.repository.RefreshTokenRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final RefreshTokenRepository refreshTokenRepository;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepository) {
                this.authenticationManager = authenticationManager;
                this.jwtTokenProvider = jwtTokenProvider;
                this.refreshTokenRepository = refreshTokenRepository;
        }

        public TokenResponseDTO login(String email, String password) {
                Authentication authentication = authenticationManager
                                .authenticate(new UsernamePasswordAuthenticationToken(
                                                email,
                                                password));

                // [유지] Access Token 생성
                String accessToken = jwtTokenProvider.createAccessToken(authentication);

                // [유지] Refresh Token 생성
                String newRefreshTokenValue = jwtTokenProvider.createRefreshToken();

                // [유지] Refresh Token 만료시간(ms) → Redis TTL(초)
                long ttlSeconds = jwtTokenProvider.getRefreshTokenValidityMilliseconds() / 1000;

               // [수정] 기존 토큰 삭제 + 새 토큰 저장을 Lua Script로 한 번에 처리
                refreshTokenRepository.save(
                                newRefreshTokenValue,
                                email,
                                ttlSeconds);

                return new TokenResponseDTO(
                                accessToken,
                                newRefreshTokenValue);
        }

        public void logout(String refreshTokenValue) {

                // [수정] 양방향 Key를 Lua Script로 함께 삭제
                refreshTokenRepository.delete(refreshTokenValue);
        }
}