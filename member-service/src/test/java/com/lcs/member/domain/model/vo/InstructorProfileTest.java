package com.lcs.member.domain.model.vo;

import com.lcs.member.domain.exception.MemberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstructorProfileTest {

    @Test
    @DisplayName("유효한 값으로 강사 프로필이 생성된다")
    void givenValidValues_whenCreateProfile_thenSucceeds() {
        InstructorProfile profile = InstructorProfile.of("홍길동", "/images/profile.jpg", "안녕하세요");
        assertEquals("홍길동", profile.getName());
        assertEquals("/images/profile.jpg", profile.getProfileImageUrl());
        assertEquals("안녕하세요", profile.getIntroduction());
    }

    @Test
    @DisplayName("프로필 사진 URL이 null이어도 생성된다")
    void givenNullProfileImageUrl_whenCreateProfile_thenSucceeds() {
        assertDoesNotThrow(() -> InstructorProfile.of("홍길동", null, "자기소개"));
    }

    @Test
    @DisplayName("자기소개가 null이어도 생성된다")
    void givenNullIntroduction_whenCreateProfile_thenSucceeds() {
        InstructorProfile profile = InstructorProfile.of("홍길동", null, null);
        assertNull(profile.getIntroduction());
    }

    @Test
    @DisplayName("이름이 null이면 예외가 발생한다")
    void givenNullName_whenCreateProfile_thenThrowsException() {
        assertThrows(MemberException.class, () -> InstructorProfile.of(null, null, null));
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
    void givenEmptyName_whenCreateProfile_thenThrowsException() {
        assertThrows(MemberException.class, () -> InstructorProfile.of("", null, null));
    }

    @Test
    @DisplayName("이름이 공백 문자열이면 예외가 발생한다")
    void givenBlankName_whenCreateProfile_thenThrowsException() {
        assertThrows(MemberException.class, () -> InstructorProfile.of("   ", null, null));
    }
}
