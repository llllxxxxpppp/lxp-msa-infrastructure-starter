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
 * MEMBER-13/MEMBER-14+15: {@code members} 테이블에 매핑되는 JPA 엔티티.
 *
 * <p>도메인 {@code Member}는 MEMBER-14+15에서 JPA 어노테이션이 전부 제거된 순수 POJO가
 * 되었고, 실제 영속성은 이 클래스(및 서브타입)와 {@link MemberRepositoryAdapter}가 담당한다.
 * 이 클래스는 순수 영속성 모델로서 도메인 규칙(불변식 검증 등)을 갖지 않는다.</p>
 */
@Entity
@Table(name = "members")
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

    void setId(Long id) {
        this.id = id;
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
