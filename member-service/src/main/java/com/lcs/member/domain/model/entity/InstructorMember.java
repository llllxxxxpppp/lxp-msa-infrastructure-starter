package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.vo.InstructorProfile;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("INSTRUCTOR")
public class InstructorMember extends Member {

    @Embedded
    private InstructorProfile profile;

    protected InstructorMember() {}

    private InstructorMember(String email, String encodedPassword, InstructorProfile profile) {
        super(email, encodedPassword);
        this.profile = profile;
    }

    public static InstructorMember create(String email, String encodedPassword,
            String name, String profileImageUrl, String introduction) {
        return new InstructorMember(email, encodedPassword,
                InstructorProfile.of(name, profileImageUrl, introduction));
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
