package com.lcs.member.infrastructure.persistence;

import com.lcs.member.domain.model.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * MEMBER-13: 헥사고날 전환 1단계로 신설된 JPA 엔티티(임시 테이블 {@code members_staging}).
 *
 * <p>기존 도메인 {@code Member}({@code members} 테이블)와는 완전히 독립된 별도 매핑이며,
 * 이 클래스는 순수 영속성 모델로서 도메인 규칙(불변식 검증 등)을 갖지 않는다.</p>
 */
@Entity
@Table(name = "members_staging")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public abstract class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean deleted;

    @Column
    private OffsetDateTime suspendedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    protected MemberJpaEntity() {}

    protected MemberJpaEntity(
            String email,
            String password,
            boolean deleted,
            OffsetDateTime suspendedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.email = email;
        this.password = password;
        this.deleted = deleted;
        this.suspendedAt = suspendedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public abstract MemberRole getRole();

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public OffsetDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
