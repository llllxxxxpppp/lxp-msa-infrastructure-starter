package com.lcs.member.domain.model.vo;

import com.lcs.member.domain.exception.MemberException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class InstructorProfile {

    @Column(name = "profile_name")
    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "profile_introduction", columnDefinition = "TEXT")
    private String introduction;

    protected InstructorProfile() {}

    public static InstructorProfile of(String name, String profileImageUrl, String introduction) {
        if (name == null || name.isBlank()) {
            throw new MemberException("강사 프로필 이름은 비어있을 수 없습니다.");
        }
        InstructorProfile profile = new InstructorProfile();
        profile.name = name;
        profile.profileImageUrl = profileImageUrl;
        profile.introduction = introduction;
        return profile;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getIntroduction() {
        return introduction;
    }
}
