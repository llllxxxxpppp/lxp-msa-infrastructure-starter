package com.lcs.auth.repository;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    // Refresh Token으로 email을 찾기 위한 Key prefix
    private static final String TOKEN_KEY_PREFIX = "refresh:token:";

    // email로 Refresh Token을 찾기 위한 역방향 Key prefix
    private static final String EMAIL_KEY_PREFIX = "refresh:email:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Refresh Token을 Redis에 양방향으로 저장한다.
     * 두 Key 모두 Refresh Token 만료시간과 동일한 TTL을 적용한다.
     */
    public void save(String refreshToken, String email, long ttlSeconds) {

        String tokenKey = TOKEN_KEY_PREFIX + refreshToken;
        String emailKey = EMAIL_KEY_PREFIX + email;

        Duration ttl = Duration.ofSeconds(ttlSeconds);

        // Refresh Token으로 email 조회용
        redisTemplate.opsForValue().set(
                tokenKey,
                email,
                ttl);

        // email로 기존 Refresh Token 조회용
        // 중복 로그인 시 기존 Refresh Token을 찾기 위해 사용
        redisTemplate.opsForValue().set(
                emailKey,
                refreshToken,
                ttl);
    }

    /**
     * Refresh Token으로 email을 조회한다.
     */
    public Optional<String> findEmailByToken(String refreshToken) {

        String tokenKey = TOKEN_KEY_PREFIX + refreshToken;

        String email = redisTemplate.opsForValue().get(tokenKey);

        if (email == null) {
            return Optional.empty();
        }

        return Optional.of(email);
    }

    /**
     * email로 기존 Refresh Token을 조회한다.
     *
     * 로그인 시 기존 Refresh Token이 있는지 확인하기 위해 사용한다.
     */
    public Optional<String> findTokenByEmail(String email) {

        String emailKey = EMAIL_KEY_PREFIX + email;

        String refreshToken = redisTemplate.opsForValue().get(emailKey);

        if (refreshToken == null) {
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }

    /**
     * Refresh Token 삭제 시 양방향 Key를 모두 삭제한다.
     *
     * 1. token Key를 통해 email 조회
     * 2. refresh:token:{refreshToken} 삭제
     * 3. refresh:email:{email} 삭제
     */
    public void delete(String refreshToken) {

        String tokenKey = TOKEN_KEY_PREFIX + refreshToken;

        // token -> email Key를 삭제하기 전에 email을 먼저 조회
        String email = redisTemplate.opsForValue().get(tokenKey);

        // Refresh Token Key 삭제
        redisTemplate.delete(tokenKey);

        // email을 찾았다면 역방향 Key도 같이 삭제
        if (email != null) {
            String emailKey = EMAIL_KEY_PREFIX + email;
            redisTemplate.delete(emailKey);
        }
    }
}