package com.lcs.gateway.jwt;

import com.lcs.gateway.jwt.exception.ExpiredTokenException;
import com.lcs.gateway.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 서명·만료를 검증하고 클레임(userId, roles)을 추출한다.
 */
@Component
public class JwtTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES = "roles";

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@code Authorization: Bearer <token>} 헤더 값에서 토큰만 분리한다.
     *
     * @param authorizationHeader Authorization 헤더 값 (null 허용)
     * @return 토큰 문자열, 형식이 맞지 않으면 null
     */
    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * 서명·만료를 검증하고 클레임을 반환한다.
     *
     * @throws ExpiredTokenException 만료된 토큰
     * @throws InvalidTokenException 서명 오류·형식 오류 등
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("Expired JWT token: " + e.getMessage(), e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    public Long getUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * roles 클레임(콤마 구분 {@code ROLE_*} 문자열)을 그대로 반환한다. (값이 없으면 빈 문자열)
     * 다운스트림은 이 값을 {@code X-Role} 헤더로 받아 인가에 사용한다.
     */
    public String getRoles(Claims claims) {
        String roles = claims.get(CLAIM_ROLES, String.class);
        return roles == null ? "" : roles;
    }
}
