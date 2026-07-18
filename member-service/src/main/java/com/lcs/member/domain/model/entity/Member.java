package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.vo.MemberId;
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
import java.util.regex.Pattern;

@Entity
@Table(name = "members")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
public abstract class Member {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,}$");

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

    protected Member() {}

    protected Member(String email, String encodedPassword) {
        validateEmail(email);
        validatePassword(encodedPassword);
        this.email = email;
        this.password = encodedPassword;
        this.deleted = false;
        this.createdAt = OffsetDateTime.now();
    }

    public abstract MemberRole getRole();

    public MemberId getId() {
        return new MemberId(id);
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

    public boolean isSuspended() {
        return suspendedAt != null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateEmail(String email) {
        validateEmail(email);
        this.email = email;
        touch();
    }

    public void updatePassword(String encodedPassword) {
        validatePassword(encodedPassword);
        this.password = encodedPassword;
        touch();
    }

    protected void markDeleted() {
        this.deleted = true;
        touch();
    }

    protected void markSuspended() {
        this.suspendedAt = OffsetDateTime.now();
        touch();
    }

    protected void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new MemberException("이메일은 비어있을 수 없습니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new MemberException("유효하지 않은 이메일 형식입니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new MemberException("패스워드는 비어있을 수 없습니다.");
        }
    }
}
