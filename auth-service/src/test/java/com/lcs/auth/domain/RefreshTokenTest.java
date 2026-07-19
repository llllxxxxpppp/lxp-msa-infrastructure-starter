package com.lcs.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken 도메인 단위 테스트")
class RefreshTokenTest {

    @Test
    @DisplayName("만료 시각이 과거이면 isExpired()는 true를 반환한다")
    void isExpired_pastExpiryDate_returnsTrue() {
        RefreshToken token = new RefreshToken("user@test.com", "value", Instant.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("만료 시각이 미래이면 isExpired()는 false를 반환한다")
    void isExpired_futureExpiryDate_returnsFalse() {
        RefreshToken token = new RefreshToken("user@test.com", "value", Instant.now().plusSeconds(60));

        assertThat(token.isExpired()).isFalse();
    }
}
