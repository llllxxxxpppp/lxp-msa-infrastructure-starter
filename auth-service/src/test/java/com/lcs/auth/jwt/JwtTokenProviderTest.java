package com.lcs.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lcs.auth.exception.ExpiredJwtCustomException;
import com.lcs.auth.exception.InvalidJwtCustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private static final String SECRET =
            "4d553a82c87c2a2e0b7000d63eb926f3ef75fd528977b9c956efcec692845953";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpireTime", 3_600_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpireTime", 864_000_000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("액세스 토큰에는 사용자 이름과 권한 정보가 클레임으로 포함된다")
    void createAccessToken_containsUsernameAndAuthorities() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtTokenProvider.createAccessToken(authentication);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertThat(claims.getSubject()).isEqualTo("user@test.com");
        assertThat(claims.get("roles", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("리프레시 토큰은 서명 검증을 통과하는 유효한 JWT로 생성된다")
    void createRefreshToken_isValid() {
        String token = jwtTokenProvider.createRefreshToken();

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰을 검증하면 ExpiredJwtCustomException이 발생한다")
    void validateToken_expiredToken_throwsExpiredJwtCustomException() {
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpireTime", -1_000L);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of());
        String expiredToken = jwtTokenProvider.createAccessToken(authentication);

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                .isInstanceOf(ExpiredJwtCustomException.class);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰을 검증하면 InvalidJwtCustomException이 발생한다")
    void validateToken_malformedToken_throwsInvalidJwtCustomException() {
        assertThatThrownBy(() -> jwtTokenProvider.validateToken("not-a-jwt-token"))
                .isInstanceOf(InvalidJwtCustomException.class);
    }

    @Test
    @DisplayName("서명이 위조된 토큰을 검증하면 InvalidJwtCustomException이 발생한다")
    void validateToken_tamperedSignature_throwsInvalidJwtCustomException() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of());
        String token = jwtTokenProvider.createAccessToken(authentication);
        int lastDot = token.lastIndexOf('.');
        char signatureFirstChar = token.charAt(lastDot + 1);
        char replacement = signatureFirstChar == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, lastDot + 1)
                + replacement
                + token.substring(lastDot + 2);

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tampered))
                .isInstanceOf(InvalidJwtCustomException.class);
    }
}
