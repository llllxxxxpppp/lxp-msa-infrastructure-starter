package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.model.MemberRole;
import java.time.OffsetDateTime;

public class AdminMember extends Member {

    protected AdminMember() {}

    private AdminMember(String email, String encodedPassword) {
        super(email, encodedPassword);
    }

    private AdminMember(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        super(id, email, password, deleted, suspendedAt, createdAt, updatedAt);
    }

    public static AdminMember create(String email, String encodedPassword) {
        return new AdminMember(email, encodedPassword);
    }

    /**
     * 이미 검증된 영속 데이터를 복원(reconstitute)하기 위한 팩토리 메서드.
     */
    public static AdminMember reconstitute(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new AdminMember(id, email, password, deleted, suspendedAt, createdAt, updatedAt);
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.ADMIN;
    }
}
