package com.lcs.member.application.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lcs.member.domain.model.entity.AdminMember;
import com.lcs.member.domain.model.entity.InstructorMember;
import com.lcs.member.domain.model.entity.RegularMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserResponseDTOTest {

    private static final String TEST_EMAIL = "user@example.com";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final Long TEST_ID = 1L;

    @Test
    @DisplayName("Given regular member not withdrawn, when UserResponseDTO.from() is called, then email should not be masked")
    void givenRegularMemberNotWithdrawn_whenFromCalled_thenEmailIsNotMasked() {
        // Given
        RegularMember member = RegularMember.create(TEST_EMAIL, ENCODED_PASSWORD);
        ReflectionTestUtils.setField(member, "id", TEST_ID);

        // When
        UserResponseDTO dto = UserResponseDTO.from(member);

        // Then
        assertEquals(TEST_EMAIL, dto.email());
    }

    @Test
    @DisplayName("Given regular member withdrawn, when UserResponseDTO.from() is called, then email should be masked with first 3 characters and asterisks")
    void givenRegularMemberWithdrawn_whenFromCalled_thenEmailIsMasked() {
        // Given
        RegularMember member = RegularMember.create(TEST_EMAIL, ENCODED_PASSWORD);
        ReflectionTestUtils.setField(member, "id", TEST_ID);
        member.withdraw();

        // When
        UserResponseDTO dto = UserResponseDTO.from(member);

        // Then
        // Original email: "user@example.com" (16 characters)
        // Expected masked: "use*************" (16 characters, first 3 chars + 13 asterisks)
        String expectedMaskedEmail = "use*************";
        assertEquals(expectedMaskedEmail, dto.email());
    }

    @Test
    @DisplayName("Given suspended instructor, when UserResponseDTO.from() is called, then email should not be masked")
    void givenSuspendedInstructor_whenFromCalled_thenEmailIsNotMasked() {
        // Given
        InstructorMember instructor = InstructorMember.create(TEST_EMAIL, ENCODED_PASSWORD,
                "Test Instructor", "http://example.com/profile.jpg", "Experienced instructor");
        ReflectionTestUtils.setField(instructor, "id", TEST_ID);
        instructor.suspend();

        // When
        UserResponseDTO dto = UserResponseDTO.from(instructor);

        // Then
        assertEquals(TEST_EMAIL, dto.email());
    }

    @Test
    @DisplayName("Given admin member, when UserResponseDTO.from() is called, then email should not be masked")
    void givenAdminMember_whenFromCalled_thenEmailIsNotMasked() {
        // Given
        AdminMember admin = AdminMember.create(TEST_EMAIL, ENCODED_PASSWORD);
        ReflectionTestUtils.setField(admin, "id", TEST_ID);

        // When
        UserResponseDTO dto = UserResponseDTO.from(admin);

        // Then
        assertEquals(TEST_EMAIL, dto.email());
    }
}
