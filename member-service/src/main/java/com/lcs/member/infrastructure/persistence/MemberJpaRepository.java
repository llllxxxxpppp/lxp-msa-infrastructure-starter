package com.lcs.member.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MEMBER-13/MEMBER-14+15: {@code members} 테이블에 매핑된 {@link MemberJpaEntity}용 Spring Data 리포지토리.
 */
public interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {
    boolean existsByEmail(String email);

    Optional<MemberJpaEntity> findByEmail(String email);
}
