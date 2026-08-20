package com.lcs.auth.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    // Refresh Token으로 email을 찾기 위한 Key prefix
    private static final String TOKEN_KEY_PREFIX = "refresh:token:";

    // email로 Refresh Token을 찾기 위한 역방향 Key prefix
    private static final String EMAIL_KEY_PREFIX = "refresh:email:";

    // [추가] 기존 토큰 삭제 + 새 양방향 Key 저장을 원자적으로 처리
    private static final DefaultRedisScript<Long> SAVE_SCRIPT = new DefaultRedisScript<>(
            """
                    local oldToken = redis.call('GET', KEYS[1])

                    if oldToken then
                        redis.call('DEL', ARGV[4] .. oldToken)
                    end

                    redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
                    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])

                    return 1
                    """,
            Long.class);

    // [추가] token Key와 email Key 삭제를 원자적으로 처리
    private static final DefaultRedisScript<Long> DELETE_SCRIPT = new DefaultRedisScript<>(
            """
                    local email = redis.call('GET', KEYS[1])

                    if not email then
                        return 0
                    end

                    local emailKey = ARGV[2] .. email
                    local currentToken = redis.call('GET', emailKey)

                    redis.call('DEL', KEYS[1])

                    if currentToken == ARGV[1] then
                        redis.call('DEL', emailKey)
                    end

                    return 1
                    """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * [수정] 기존 Refresh Token을 삭제하고
     * 새 Refresh Token을 양방향 Key로 저장한다.
     */
    public void save(String refreshToken, String email, long ttlSeconds) {

        String tokenKey = TOKEN_KEY_PREFIX + refreshToken;
        String emailKey = EMAIL_KEY_PREFIX + email;

        // [수정] 조회/삭제/저장을 Lua Script로 한 번에 처리
        redisTemplate.execute(
                SAVE_SCRIPT,
                List.of(emailKey, tokenKey),
                refreshToken,
                email,
                String.valueOf(ttlSeconds),
                TOKEN_KEY_PREFIX);
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
     * [수정] Refresh Token의 양방향 Key를 함께 삭제한다.
     */
    public void delete(String refreshToken) {

        String tokenKey = TOKEN_KEY_PREFIX + refreshToken;

        // [수정] 양방향 Key 삭제를 Lua Script로 한 번에 처리
        redisTemplate.execute(
                DELETE_SCRIPT,
                List.of(tokenKey),
                refreshToken,
                EMAIL_KEY_PREFIX);
    }
}