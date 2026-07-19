package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.vo.InstructorProfile;
import java.time.OffsetDateTime;

public class InstructorMember extends Member {

    private InstructorProfile profile;

    protected InstructorMember() {}

    private InstructorMember(String email, String encodedPassword, InstructorProfile profile) {
        super(email, encodedPassword);
        this.profile = profile;
    }

    private InstructorMember(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            InstructorProfile profile) {
        super(id, email, password, deleted, suspendedAt, createdAt, updatedAt);
        this.profile = profile;
    }

    public static InstructorMember create(String email, String encodedPassword,
            String name, String profileImageUrl, String introduction) {
        return new InstructorMember(email, encodedPassword,
                InstructorProfile.of(name, profileImageUrl, introduction));
    }

    /**
     * 이미 검증된 영속 데이터를 복원(reconstitute)하기 위한 팩토리 메서드.
     */
    public static InstructorMember reconstitute(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            String profileName, String profileImageUrl, String profileIntroduction) {
        InstructorProfile profile = InstructorProfile.of(profileName, profileImageUrl, profileIntroduction);
        return new InstructorMember(id, email, password, deleted, suspendedAt, createdAt, updatedAt, profile);
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.INSTRUCTOR;
    }

    public InstructorProfile getProfile() {
        return profile;
    }

    public void updateProfile(String name, String profileImageUrl, String introduction) {
        this.profile = InstructorProfile.of(name, profileImageUrl, introduction);
        touch();
    }

    public void suspend() {
        markSuspended();
        markDeleted();
    }
}
