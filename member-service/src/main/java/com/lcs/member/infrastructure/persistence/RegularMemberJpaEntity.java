package com.lcs.member.infrastructure.persistence;

import com.lcs.member.domain.model.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;

/**
 * MEMBER-13: 임시 테이블 {@code members_staging}에 매핑되는 일반 회원 JPA 엔티티({@code role = MEMBER}).
 */
@Entity
@DiscriminatorValue("MEMBER")
public class RegularMemberJpaEntity extends MemberJpaEntity {

    @Column
    private OffsetDateTime withdrawnAt;

    protected RegularMemberJpaEntity() {}

    public RegularMemberJpaEntity(
            String email,
            String password,
            boolean deleted,
            OffsetDateTime suspendedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime withdrawnAt) {
        super(email, password, deleted, suspendedAt, createdAt, updatedAt);
        this.withdrawnAt = withdrawnAt;
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.MEMBER;
    }

    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }
}
