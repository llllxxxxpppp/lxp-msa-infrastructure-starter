package com.lcs.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenRepository 단위 테스트")
class RefreshTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("save는 refresh:token:{token}->email과 refresh:email:{email}->token 양방향 Key를 동일 TTL로 저장한다")
    void save_storesTokenAsKeyAndEmailAsValueWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        refreshTokenRepository.save("token-value", "user@test.com", 600L);

        verify(valueOperations).set(
                "refresh:token:token-value", "user@test.com", Duration.ofSeconds(600L));
        verify(valueOperations).set(
                "refresh:email:user@test.com", "token-value", Duration.ofSeconds(600L));
    }

    @Test
    @DisplayName("findEmailByToken은 refresh:token:{token} Key로 저장된 토큰이면 email을 반환한다")
    void findEmailByToken_existingToken_returnsEmail() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);
        given(valueOperations.get("refresh:token:token-value")).willReturn("user@test.com");

        Optional<String> email = refreshTokenRepository.findEmailByToken("token-value");

        assertThat(email).contains("user@test.com");
    }

    @Test
    @DisplayName("findEmailByToken은 존재하지 않는 토큰이면 빈 Optional을 반환한다")
    void findEmailByToken_unknownToken_returnsEmpty() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);
        given(valueOperations.get("refresh:token:no-such-token")).willReturn(null);

        Optional<String> email = refreshTokenRepository.findEmailByToken("no-such-token");

        assertThat(email).isEmpty();
    }

    @Test
    @DisplayName("findTokenByEmail은 refresh:email:{email} Key로 저장된 토큰을 반환한다")
    void findTokenByEmail_existingEmail_returnsToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);
        given(valueOperations.get("refresh:email:user@test.com")).willReturn("token-value");

        Optional<String> token = refreshTokenRepository.findTokenByEmail("user@test.com");

        assertThat(token).contains("token-value");
    }

    @Test
    @DisplayName("delete는 refresh:token:{token}과 refresh:email:{email} Key를 모두 Redis에서 삭제한다")
    void delete_removesBothKeysFromRedis() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:token:token-value")).willReturn("user@test.com");
        refreshTokenRepository = new RefreshTokenRepository(redisTemplate);

        refreshTokenRepository.delete("token-value");

        verify(redisTemplate).delete("refresh:token:token-value");
        verify(redisTemplate).delete("refresh:email:user@test.com");
    }
}
