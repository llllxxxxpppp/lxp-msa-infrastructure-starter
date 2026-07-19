package com.lcs.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lcs.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@DisplayName("RefreshTokenRepository 통합 테스트")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("저장한 토큰은 findByToken으로 조회된다")
    void save_and_findByToken_returnsSavedEntity() {
        refreshTokenRepository.save(
                new RefreshToken("user@test.com", "token-value", Instant.now().plusSeconds(600)));

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-value");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("저장한 토큰은 findByEmail로 조회된다")
    void findByEmail_returnsSavedEntity() {
        refreshTokenRepository.save(
                new RefreshToken("user2@test.com", "token-2", Instant.now().plusSeconds(600)));

        assertThat(refreshTokenRepository.findByEmail("user2@test.com")).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 토큰을 조회하면 빈 Optional을 반환한다")
    void findByToken_unknownToken_returnsEmpty() {
        assertThat(refreshTokenRepository.findByToken("no-such-token")).isEmpty();
    }

    @Test
    @DisplayName("동일한 이메일로 두 번 저장하면 unique 제약조건 위반이 발생한다")
    void duplicateEmail_violatesUniqueConstraint() {
        refreshTokenRepository.saveAndFlush(
                new RefreshToken("dup@test.com", "token-a", Instant.now().plusSeconds(600)));

        RefreshToken duplicate = new RefreshToken("dup@test.com", "token-b", Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
