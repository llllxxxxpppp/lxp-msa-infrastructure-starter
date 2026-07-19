package com.lcs.member.infrastructure.persistence;

import com.lcs.member.domain.model.MemberRole;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;

/**
 * MEMBER-13: 임시 테이블 {@code members_staging}에 매핑되는 관리자 회원 JPA 엔티티({@code role = ADMIN}).
 */
@Entity
@DiscriminatorValue("ADMIN")
public class AdminMemberJpaEntity extends MemberJpaEntity {

    protected AdminMemberJpaEntity() {}

    public AdminMemberJpaEntity(
            String email,
            String password,
            boolean deleted,
            OffsetDateTime suspendedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        super(email, password, deleted, suspendedAt, createdAt, updatedAt);
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.ADMIN;
    }
}
