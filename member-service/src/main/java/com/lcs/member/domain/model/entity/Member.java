package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.vo.MemberId;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public abstract class Member {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,}$");

    private Long id;

    private String email;

    private String password;

    private boolean deleted;

    private OffsetDateTime suspendedAt;

    private OffsetDateTime createdAt;

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

    /**
     * 이미 검증된 영속 데이터를 복원(reconstitute)하기 위한 생성자.
     * 검증 없이 그대로 대입한다.
     */
    protected Member(Long id, String email, String password, boolean deleted,
            OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.deleted = deleted;
        this.suspendedAt = suspendedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public abstract MemberRole getRole();

    public boolean isPersisted() {
        return id != null;
    }

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
