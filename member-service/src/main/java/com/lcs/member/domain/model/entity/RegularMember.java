package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.model.MemberRole;
import java.time.OffsetDateTime;

public class RegularMember extends Member {

    private OffsetDateTime withdrawnAt;

    protected RegularMember() {}

    private RegularMember(String email, String encodedPassword) {
        super(email, encodedPassword);
    }

    private RegularMember(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        super(id, email, password, deleted, suspendedAt, createdAt, updatedAt);
    }

    public static RegularMember create(String email, String encodedPassword) {
        return new RegularMember(email, encodedPassword);
    }

    /**
     * 이미 검증된 영속 데이터를 복원(reconstitute)하기 위한 팩토리 메서드.
     */
    public static RegularMember reconstitute(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            OffsetDateTime withdrawnAt) {
        RegularMember member = new RegularMember(id, email, password, deleted, suspendedAt, createdAt, updatedAt);
        member.withdrawnAt = withdrawnAt;
        return member;
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.MEMBER;
    }

    public OffsetDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void withdraw() {
        markDeleted();
        this.withdrawnAt = OffsetDateTime.now();
    }

    public void suspend() {
        markSuspended();
        markDeleted();
    }
}
