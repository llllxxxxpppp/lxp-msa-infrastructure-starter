package com.lcs.member.infrastructure.persistence;

import com.lcs.member.domain.model.entity.AdminMember;
import com.lcs.member.domain.model.entity.InstructorMember;
import com.lcs.member.domain.model.entity.Member;
import com.lcs.member.domain.model.entity.RegularMember;
import com.lcs.member.domain.repository.MemberRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * MEMBER-14+15: 헥사고날 전환 2단계 - {@code domain.repository.MemberRepository} 포트의
 * 실제 구현. 순수 POJO가 된 도메인 {@code Member} 계층과 {@link MemberJpaEntity} 계층
 * (및 서브타입) 사이의 양방향 변환을 담당한다.
 */
@Repository
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    public MemberRepositoryAdapter(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Member> T save(T member) {
        MemberJpaEntity jpaEntity = toJpaEntity(member);
        if (member.isPersisted()) {
            jpaEntity.setId(member.getId().value());
        }
        MemberJpaEntity saved = memberJpaRepository.save(jpaEntity);
        return (T) toDomain(saved);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberJpaRepository.findByEmail(email).map(this::toDomain);
    }

    private MemberJpaEntity toJpaEntity(Member member) {
        if (member instanceof RegularMember regularMember) {
            return new RegularMemberJpaEntity(
                    regularMember.getEmail(),
                    regularMember.getPassword(),
                    regularMember.isDeleted(),
                    regularMember.getSuspendedAt(),
                    regularMember.getCreatedAt(),
                    regularMember.getUpdatedAt(),
                    regularMember.getWithdrawnAt());
        }
        if (member instanceof InstructorMember instructorMember) {
            return new InstructorMemberJpaEntity(
                    instructorMember.getEmail(),
                    instructorMember.getPassword(),
                    instructorMember.isDeleted(),
                    instructorMember.getSuspendedAt(),
                    instructorMember.getCreatedAt(),
                    instructorMember.getUpdatedAt(),
                    instructorMember.getProfile().getName(),
                    instructorMember.getProfile().getProfileImageUrl(),
                    instructorMember.getProfile().getIntroduction());
        }
        if (member instanceof AdminMember adminMember) {
            return new AdminMemberJpaEntity(
                    adminMember.getEmail(),
                    adminMember.getPassword(),
                    adminMember.isDeleted(),
                    adminMember.getSuspendedAt(),
                    adminMember.getCreatedAt(),
                    adminMember.getUpdatedAt());
        }
        throw new IllegalStateException("알 수 없는 Member 서브타입입니다: " + member.getClass());
    }

    private Member toDomain(MemberJpaEntity jpaEntity) {
        if (jpaEntity instanceof RegularMemberJpaEntity regularMemberJpaEntity) {
            return RegularMember.reconstitute(
                    regularMemberJpaEntity.getId(),
                    regularMemberJpaEntity.getEmail(),
                    regularMemberJpaEntity.getPassword(),
                    regularMemberJpaEntity.isDeleted(),
                    regularMemberJpaEntity.getSuspendedAt(),
                    regularMemberJpaEntity.getCreatedAt(),
                    regularMemberJpaEntity.getUpdatedAt(),
                    regularMemberJpaEntity.getWithdrawnAt());
        }
        if (jpaEntity instanceof InstructorMemberJpaEntity instructorMemberJpaEntity) {
            return InstructorMember.reconstitute(
                    instructorMemberJpaEntity.getId(),
                    instructorMemberJpaEntity.getEmail(),
                    instructorMemberJpaEntity.getPassword(),
                    instructorMemberJpaEntity.isDeleted(),
                    instructorMemberJpaEntity.getSuspendedAt(),
                    instructorMemberJpaEntity.getCreatedAt(),
                    instructorMemberJpaEntity.getUpdatedAt(),
                    instructorMemberJpaEntity.getProfileName(),
                    instructorMemberJpaEntity.getProfileImageUrl(),
                    instructorMemberJpaEntity.getProfileIntroduction());
        }
        if (jpaEntity instanceof AdminMemberJpaEntity adminMemberJpaEntity) {
            return AdminMember.reconstitute(
                    adminMemberJpaEntity.getId(),
                    adminMemberJpaEntity.getEmail(),
                    adminMemberJpaEntity.getPassword(),
                    adminMemberJpaEntity.isDeleted(),
                    adminMemberJpaEntity.getSuspendedAt(),
                    adminMemberJpaEntity.getCreatedAt(),
                    adminMemberJpaEntity.getUpdatedAt());
        }
        throw new IllegalStateException("알 수 없는 MemberJpaEntity 서브타입입니다: " + jpaEntity.getClass());
    }
}
