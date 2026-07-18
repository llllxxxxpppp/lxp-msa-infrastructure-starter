package com.lcs.member.domain.model.entity;

import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.vo.InstructorProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    // -------------------------------------------------------------------------
    // RegularMember.create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 이메일과 패스워드로 일반 회원이 생성된다")
    void givenValidEmailAndPassword_whenCreateMember_thenRoleIsMember() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        assertEquals(MemberRole.MEMBER, member.getRole());
    }

    @Test
    @DisplayName("일반 회원 생성 시 수정 일자는 null이다")
    void givenValidValues_whenCreateMember_thenUpdatedAtIsNull() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        assertNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("일반 회원 생성 시 탈퇴 상태는 false이다")
    void givenValidValues_whenCreateMember_thenDeletedIsFalse() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        assertFalse(member.isDeleted());
    }

    @Test
    @DisplayName("일반 회원 생성 시 정지일시와 탈퇴일시는 null이다")
    void givenValidValues_whenCreateRegularMember_thenSuspendedAtAndWithdrawnAtAreNull() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        assertNull(member.getSuspendedAt());
        assertNull(member.getWithdrawnAt());
    }

    @Test
    @DisplayName("이메일이 null이면 일반 회원 생성 시 예외가 발생한다")
    void givenNullEmail_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> RegularMember.create(null, "encoded_password"));
    }

    @Test
    @DisplayName("이메일이 빈 문자열이면 일반 회원 생성 시 예외가 발생한다")
    void givenBlankEmail_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> RegularMember.create("   ", "encoded_password"));
    }

    @Test
    @DisplayName("이메일 형식이 유효하지 않으면 일반 회원 생성 시 예외가 발생한다")
    void givenInvalidEmail_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> RegularMember.create("not-an-email", "encoded_password"));
    }

    @Test
    @DisplayName("패스워드가 null이면 일반 회원 생성 시 예외가 발생한다")
    void givenNullPassword_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> RegularMember.create("user@example.com", null));
    }

    @Test
    @DisplayName("패스워드가 빈 문자열이면 일반 회원 생성 시 예외가 발생한다")
    void givenBlankPassword_whenCreateMember_thenThrowsException() {
        assertThrows(MemberException.class, () -> RegularMember.create("user@example.com", "   "));
    }

    // -------------------------------------------------------------------------
    // InstructorMember.create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 값으로 강사가 생성된다")
    void givenValidValues_whenCreateInstructor_thenRoleIsInstructor() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        assertEquals(MemberRole.INSTRUCTOR, instructor.getRole());
    }

    @Test
    @DisplayName("강사 생성 시 프로필 정보가 설정된다")
    void givenValidValues_whenCreateInstructor_thenProfileIsSet() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", "/images/profile.jpg", "안녕하세요");
        InstructorProfile profile = instructor.getProfile();
        assertNotNull(profile);
        assertEquals("홍길동", profile.getName());
    }

    // -------------------------------------------------------------------------
    // AdminMember.create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 값으로 어드민이 생성된다")
    void givenValidValues_whenCreateAdmin_thenRoleIsAdmin() {
        AdminMember admin = AdminMember.create("admin@example.com", "encoded_password");
        assertEquals(MemberRole.ADMIN, admin.getRole());
    }

    // -------------------------------------------------------------------------
    // updateEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 이메일로 이메일을 변경할 수 있다")
    void givenValidEmail_whenUpdateEmail_thenEmailIsUpdated() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.updateEmail("new@example.com");
        assertEquals("new@example.com", member.getEmail());
    }

    @Test
    @DisplayName("이메일 변경 후 수정 일자가 갱신된다")
    void givenValidEmail_whenUpdateEmail_thenUpdatedAtIsSet() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.updateEmail("new@example.com");
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("유효하지 않은 이메일로 변경하면 예외가 발생한다")
    void givenInvalidEmail_whenUpdateEmail_thenThrowsException() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        assertThrows(MemberException.class, () -> member.updateEmail("invalid-email"));
    }

    // -------------------------------------------------------------------------
    // updatePassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 패스워드로 패스워드를 변경할 수 있다")
    void givenValidPassword_whenUpdatePassword_thenPasswordIsUpdated() {
        RegularMember member = RegularMember.create("user@example.com", "old_password");
        member.updatePassword("new_password");
        assertEquals("new_password", member.getPassword());
    }

    @Test
    @DisplayName("패스워드 변경 후 수정 일자가 갱신된다")
    void givenValidPassword_whenUpdatePassword_thenUpdatedAtIsSet() {
        RegularMember member = RegularMember.create("user@example.com", "old_password");
        member.updatePassword("new_password");
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("패스워드가 빈 문자열이면 패스워드 변경 시 예외가 발생한다")
    void givenBlankPassword_whenUpdatePassword_thenThrowsException() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        assertThrows(MemberException.class, () -> member.updatePassword("   "));
    }

    // -------------------------------------------------------------------------
    // updateProfile
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사는 프로필을 수정할 수 있다")
    void givenInstructor_whenUpdateProfile_thenProfileIsUpdated() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        assertDoesNotThrow(() -> instructor.updateProfile("김철수", "/new_image.jpg", "새 소개"));
        assertEquals("김철수", instructor.getProfile().getName());
    }

    @Test
    @DisplayName("프로필 수정 후 수정 일자가 갱신된다")
    void givenInstructor_whenUpdateProfile_thenUpdatedAtIsSet() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        instructor.updateProfile("김철수", null, null);
        assertNotNull(instructor.getUpdatedAt());
    }

    @Test
    @DisplayName("프로필 이름이 빈 문자열이면 프로필 수정 시 예외가 발생한다")
    void givenBlankProfileName_whenUpdateProfile_thenThrowsException() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        assertThrows(MemberException.class, () -> instructor.updateProfile("", null, null));
    }

    // -------------------------------------------------------------------------
    // withdraw (RegularMember)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("일반 회원 탈퇴 처리 시 deleted가 true가 된다")
    void givenRegularMember_whenWithdraw_thenDeletedIsTrue() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.withdraw();
        assertTrue(member.isDeleted());
    }

    @Test
    @DisplayName("일반 회원 탈퇴 처리 시 탈퇴일시가 설정된다")
    void givenRegularMember_whenWithdraw_thenWithdrawnAtIsSet() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.withdraw();
        assertNotNull(member.getWithdrawnAt());
    }

    @Test
    @DisplayName("일반 회원 탈퇴 처리 후 수정 일자가 갱신된다")
    void givenRegularMember_whenWithdraw_thenUpdatedAtIsSet() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.withdraw();
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("일반 회원 탈퇴 처리 후에도 정지일시는 null로 유지된다")
    void givenRegularMember_whenWithdraw_thenSuspendedAtRemainsNull() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.withdraw();
        assertNull(member.getSuspendedAt());
    }

    // -------------------------------------------------------------------------
    // suspend (RegularMember)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("일반 회원 정지 처리 시 정지일시가 설정된다")
    void givenRegularMember_whenSuspend_thenSuspendedAtIsSet() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.suspend();
        assertNotNull(member.getSuspendedAt());
    }

    @Test
    @DisplayName("일반 회원 정지 처리 시 로그인 차단을 위해 deleted가 true가 된다")
    void givenRegularMember_whenSuspend_thenDeletedIsTrue() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.suspend();
        assertTrue(member.isDeleted());
    }

    @Test
    @DisplayName("일반 회원 정지 처리 시 수정 일자가 갱신된다")
    void givenRegularMember_whenSuspend_thenUpdatedAtIsSet() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.suspend();
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("일반 회원 정지 처리 후에도 탈퇴일시는 null로 유지된다")
    void givenRegularMember_whenSuspend_thenWithdrawnAtRemainsNull() {
        RegularMember member = RegularMember.create("user@example.com", "encoded_password");
        member.suspend();
        assertNull(member.getWithdrawnAt());
    }

    // -------------------------------------------------------------------------
    // suspend (InstructorMember)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사 정지 처리 시 deleted가 true가 되고 정지일시가 설정된다")
    void givenInstructor_whenSuspend_thenDeletedIsTrueAndSuspendedAtIsSet() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        instructor.suspend();
        assertTrue(instructor.isDeleted());
        assertNotNull(instructor.getSuspendedAt());
    }

    @Test
    @DisplayName("강사 정지 처리 후 수정 일자가 갱신된다")
    void givenInstructor_whenSuspend_thenUpdatedAtIsSet() {
        InstructorMember instructor = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);
        instructor.suspend();
        assertNotNull(instructor.getUpdatedAt());
    }
}
