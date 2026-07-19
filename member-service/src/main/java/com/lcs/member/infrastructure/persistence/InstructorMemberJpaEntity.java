package com.lcs.member.infrastructure.persistence;

import com.lcs.member.domain.model.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.OffsetDateTime;

/**
 * MEMBER-13: 임시 테이블 {@code members_staging}에 매핑되는 강사 회원 JPA 엔티티({@code role = INSTRUCTOR}).
 *
 * <p>프로필은 도메인 {@code InstructorProfile} VO를 {@code @Embedded}로 재사용하지 않고,
 * 평범한 String 필드 3개로 매핑한다.</p>
 */
@Entity
@DiscriminatorValue("INSTRUCTOR")
public class InstructorMemberJpaEntity extends MemberJpaEntity {

    @Column(name = "profile_name")
    private String profileName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "profile_introduction", columnDefinition = "TEXT")
    private String profileIntroduction;

    protected InstructorMemberJpaEntity() {}

    public InstructorMemberJpaEntity(
            String email,
            String password,
            boolean deleted,
            OffsetDateTime suspendedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String profileName,
            String profileImageUrl,
            String profileIntroduction) {
        super(email, password, deleted, suspendedAt, createdAt, updatedAt);
        this.profileName = profileName;
        this.profileImageUrl = profileImageUrl;
        this.profileIntroduction = profileIntroduction;
    }

    @Override
    public MemberRole getRole() {
        return MemberRole.INSTRUCTOR;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getProfileIntroduction() {
        return profileIntroduction;
    }
}
