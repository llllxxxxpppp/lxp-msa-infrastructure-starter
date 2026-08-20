package com.lcs.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenRepository 단위 테스트")
class RefreshTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("save는 Lua Script로 기존 토큰 삭제와 양방향 Key 저장을 처리한다")
    void save_executesLuaScript() {

        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        refreshTokenRepository.save(
                "token-value",
                "user@test.com",
                600L);

        // [수정] 개별 SET 대신 Lua Script 실행 확인
        verify(redisTemplate)
                .execute(
                        any(DefaultRedisScript.class),
                        eq(
                                List.of(
                                        "refresh:email:user@test.com",
                                        "refresh:token:token-value")),
                        eq("token-value"),
                        eq("user@test.com"),
                        eq("600"),
                        eq("refresh:token:"));
    }

    @Test
    @DisplayName("findEmailByToken은 refresh:token:{token} Key로 저장된 토큰이면 email을 반환한다")
    void findEmailByToken_existingToken_returnsEmail() {

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        given(valueOperations.get("refresh:token:token-value"))
                .willReturn("user@test.com");

        Optional<String> email =
                refreshTokenRepository.findEmailByToken("token-value");

        assertThat(email).contains("user@test.com");
    }

    @Test
    @DisplayName("findEmailByToken은 존재하지 않는 토큰이면 빈 Optional을 반환한다")
    void findEmailByToken_unknownToken_returnsEmpty() {

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        given(valueOperations.get("refresh:token:no-such-token"))
                .willReturn(null);

        Optional<String> email =
                refreshTokenRepository.findEmailByToken("no-such-token");

        assertThat(email).isEmpty();
    }

    @Test
    @DisplayName("findTokenByEmail은 refresh:email:{email} Key로 저장된 토큰을 반환한다")
    void findTokenByEmail_existingEmail_returnsToken() {

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        given(valueOperations.get("refresh:email:user@test.com"))
                .willReturn("token-value");

        Optional<String> token =
                refreshTokenRepository.findTokenByEmail("user@test.com");

        assertThat(token).contains("token-value");
    }

    @Test
    @DisplayName("delete는 Lua Script로 양방향 Key 삭제를 처리한다")
    void delete_executesLuaScript() {

        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        refreshTokenRepository.delete("token-value");

        // [수정] 양방향 삭제를 Lua Script 한 번으로 처리
        verify(redisTemplate)
                .execute(
                        any(DefaultRedisScript.class),
                        eq(List.of("refresh:token:token-value")),
                        eq("token-value"),
                        eq("refresh:email:"));
    }
}