package com.lcs.member.domain.repository;

import com.lcs.member.domain.model.entity.Member;
import java.util.Optional;

/**
 * MEMBER-14+15: 헥사고날 전환 2단계 - 더 이상 {@code JpaRepository}를 상속하지 않는
 * 순수 포트 인터페이스. 실제 영속성 구현은
 * {@code infrastructure.persistence.MemberRepositoryAdapter}가 담당한다.
 */
public interface MemberRepository {

    <T extends Member> T save(T member);

    Optional<Member> findById(Long id);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);
}
