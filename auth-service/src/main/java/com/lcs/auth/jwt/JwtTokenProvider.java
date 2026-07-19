package com.lcs.auth.jwt;

import com.lcs.auth.exception.ExpiredJwtCustomException;
import com.lcs.auth.exception.InvalidJwtCustomException;
import com.lcs.auth.principal.CustomUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity-milliseconds}")
    private long accessTokenExpireTime;

    @Value("${jwt.refresh-token-validity-milliseconds}")
    private long refreshTokenExpireTime;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    public String createAccessToken(Authentication authentication) {
        String username = authentication.getName();

        String authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Long userId = null;
        if (authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            userId = principal.getUserId();
        }

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", authorities)
                .issuedAt(new Date()) // NOPMD - JJWT API requires java.util.Date
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpireTime)) // NOPMD
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public String createRefreshToken() {
        return Jwts.builder().issuedAt(new Date()) // NOPMD - JJWT API requires java.util.Date
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpireTime)) // NOPMD
                .signWith(key, Jwts.SIG.HS512).compact();
    }

    public long getRefreshTokenValidityMilliseconds() {
        return refreshTokenExpireTime;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredJwtCustomException("Expired JWT token: " + e.getMessage(), e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtCustomException("invalid Jwt Token " + e.getMessage(), e);
        }
    }
}
