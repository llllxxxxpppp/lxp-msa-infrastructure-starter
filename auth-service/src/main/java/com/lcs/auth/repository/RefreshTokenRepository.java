package com.lcs.auth.repository;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // [수정] Redis에 Refresh Token → email 저장 + TTL 설정
    public void save(String refreshToken, String email, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                refreshToken,
                email,
                Duration.ofSeconds(ttlSeconds));
    }

    // [수정] Refresh Token으로 email 조회
    public Optional<String> findEmailByToken(String refreshToken) {
        String email = redisTemplate.opsForValue().get(refreshToken);

        if (email == null) {
            return Optional.empty();
        }

        return Optional.of(email);
    }

    // [유지] 로그아웃 시 Refresh Token 삭제
    public void delete(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }
}