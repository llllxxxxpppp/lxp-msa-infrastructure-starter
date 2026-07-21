package com.lcs.member.application.service;

import com.lcs.member.application.dto.response.CreateMemberResponse;
import com.lcs.member.application.dto.response.MemberAuthStatusResponse;
import com.lcs.member.application.dto.response.MemberCredentialResponse;
import com.lcs.member.application.dto.response.SuspensionStatusResponse;
import com.lcs.member.application.dto.response.UserResponseDTO;
import com.lcs.member.domain.event.InstructorSuspendedEvent;
import com.lcs.member.domain.event.MemberRegisteredEvent;
import com.lcs.member.domain.event.MemberSuspendedEvent;
import com.lcs.member.domain.event.MemberWithdrawnEvent;
import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.entity.InstructorMember;
import com.lcs.member.domain.model.entity.RegularMember;
import com.lcs.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberService memberService;

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("이메일 중복이 없으면 회원가입 시 save와 MemberRegisteredEvent 발행이 수행된다")
    void givenNonDuplicateEmail_whenRegister_thenSaveAndPublishMemberRegisteredEventAreInvoked() {
        String email = "user@example.com";
        String password = "password123";
        String encodedPassword = "encoded_password";

        RegularMember savedMember = RegularMember.create(email, encodedPassword);
        ReflectionTestUtils.setField(savedMember, "id", 1L);

        when(memberRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(memberRepository.save(any(RegularMember.class))).thenReturn(savedMember);

        UserResponseDTO result = memberService.register(email, password);

        ArgumentCaptor<MemberRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(MemberRegisteredEvent.class);
        verify(memberRepository).save(any(RegularMember.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(1L, result.id());
        assertEquals(1L, eventCaptor.getValue().getMemberId());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입하려 하면 MemberException이 발생하고 save와 eventPublisher 호출이 수행되지 않는다")
    void givenExistingEmail_whenRegister_thenThrowsMemberExceptionAndSaveAndEventPublisherAreNotInvoked() {
        String email = "user@example.com";
        String password = "password123";

        when(memberRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(MemberException.class, () -> memberService.register(email, password));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // -------------------------------------------------------------------------
    // suspendMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("일반 회원을 정지시키면 save와 MemberSuspendedEvent 발행이 수행된다")
    void givenExistingRegularMember_whenSuspendMember_thenSaveAndPublishMemberSuspendedEventAreInvoked() {
        Long memberId = 1L;
        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        memberService.suspendMember(memberId);

        ArgumentCaptor<MemberSuspendedEvent> eventCaptor = ArgumentCaptor.forClass(MemberSuspendedEvent.class);
        verify(memberRepository).save(any(RegularMember.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(memberId, eventCaptor.getValue().getMemberId());
    }

    @Test
    @DisplayName("존재하지 않는 일반 회원을 정지시키려 하면 MemberException이 발생한다")
    void givenNonExistingMember_whenSuspendMember_thenThrowsMemberException() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () -> memberService.suspendMember(memberId));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("강사를 일반 회원으로 정지시키려 하면 MemberException이 발생한다")
    void givenInstructorMember_whenSuspendMember_thenThrowsMemberException() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        assertThrows(MemberException.class, () -> memberService.suspendMember(instructorId));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // -------------------------------------------------------------------------
    // withdrawMember
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("일반 회원을 탈퇴시키면 save와 MemberWithdrawnEvent 발행이 수행된다")
    void givenExistingRegularMember_whenWithdrawMember_thenSaveAndPublishMemberWithdrawnEventAreInvoked() {
        Long memberId = 1L;
        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        memberService.withdrawMember(memberId);

        ArgumentCaptor<MemberWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(MemberWithdrawnEvent.class);
        verify(memberRepository).save(any(RegularMember.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(memberId, eventCaptor.getValue().getMemberId());
    }

    @Test
    @DisplayName("존재하지 않는 회원을 탈퇴시키려 하면 MemberException이 발생한다")
    void givenNonExistingMember_whenWithdrawMember_thenThrowsMemberException() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () -> memberService.withdrawMember(memberId));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("강사를 일반 회원 탈퇴로 처리하려 하면 MemberException이 발생한다")
    void givenInstructorMember_whenWithdrawMember_thenThrowsMemberException() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        assertThrows(MemberException.class, () -> memberService.withdrawMember(instructorId));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // -------------------------------------------------------------------------
    // suspendInstructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사를 정지시키면 save와 InstructorSuspendedEvent 발행이 수행된다")
    void givenExistingInstructor_whenSuspendInstructor_thenSaveAndPublishInstructorSuspendedEventAreInvoked() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "홍길동", null, null);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        memberService.suspendInstructor(instructorId);

        ArgumentCaptor<InstructorSuspendedEvent> eventCaptor = ArgumentCaptor.forClass(InstructorSuspendedEvent.class);
        verify(memberRepository).save(any(InstructorMember.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(instructorId, eventCaptor.getValue().getInstructorId());
    }

    @Test
    @DisplayName("존재하지 않는 강사를 정지시키려 하면 MemberException이 발생한다")
    void givenNonExistingInstructor_whenSuspendInstructor_thenThrowsMemberException() {
        Long instructorId = 999L;

        when(memberRepository.findById(instructorId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () -> memberService.suspendInstructor(instructorId));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("일반 회원을 강사로 정지시키려 하면 MemberException이 발생한다")
    void givenRegularMember_whenSuspendInstructor_thenThrowsMemberException() {
        Long memberId = 1L;
        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThrows(MemberException.class, () -> memberService.suspendInstructor(memberId));

        verify(memberRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // -------------------------------------------------------------------------
    // registerInstructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사를 정상적으로 등록하면 save가 호출되고 UserResponseDTO를 반환한다")
    void givenValidInstructorData_whenRegisterInstructor_thenSaveIsCalledAndReturnUserResponseDTO() {
        String email = "instructor@example.com";
        String password = "password123";
        String name = "홍길동";
        String profileImageUrl = "https://example.com/image.jpg";
        String introduction = "안녕하세요, 저는 홍길동입니다.";

        InstructorMember instructorMember = InstructorMember.create(email, "encoded_password", name, profileImageUrl, introduction);
        ReflectionTestUtils.setField(instructorMember, "id", 1L);

        when(memberRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(memberRepository.save(any(InstructorMember.class))).thenReturn(instructorMember);

        UserResponseDTO result = memberService.registerInstructor(email, password, name, profileImageUrl, introduction);

        verify(memberRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(memberRepository).save(any(InstructorMember.class));

        assertEquals(1L, result.id());
        assertEquals(email, result.email());
        assertEquals(MemberRole.INSTRUCTOR, result.role());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 강사를 등록하려 하면 MemberException이 발생한다")
    void givenExistingEmail_whenRegisterInstructor_thenThrowsMemberException() {
        String email = "instructor@example.com";
        String password = "password123";
        String name = "홍길동";
        String profileImageUrl = "https://example.com/image.jpg";
        String introduction = "안녕하세요, 저는 홍길동입니다.";

        when(memberRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(MemberException.class, () ->
                memberService.registerInstructor(email, password, name, profileImageUrl, introduction));

        verify(memberRepository).existsByEmail(email);
        verify(memberRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 변경되고 save가 호출된다")
    void givenValidCurrentPassword_whenChangePassword_thenPasswordIsUpdatedAndSaveIsCalled() {
        Long memberId = 1L;
        String currentPassword = "current_password";
        String newPassword = "new_password";
        String encodedCurrentPassword = "encoded_current_password";
        String encodedNewPassword = "encoded_new_password";

        RegularMember regularMember = RegularMember.create("user@example.com", encodedCurrentPassword);
        ReflectionTestUtils.setField(regularMember, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));
        when(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        memberService.changePassword(memberId, currentPassword, newPassword);

        verify(memberRepository).findById(memberId);
        verify(passwordEncoder).matches(currentPassword, encodedCurrentPassword);
        verify(passwordEncoder).encode(newPassword);
        verify(memberRepository).save(any(RegularMember.class));
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 MemberException이 발생하고 save가 호출되지 않는다")
    void givenInvalidCurrentPassword_whenChangePassword_thenThrowsMemberExceptionAndSaveIsNotCalled() {
        Long memberId = 1L;
        String currentPassword = "wrong_password";
        String newPassword = "new_password";
        String encodedCurrentPassword = "encoded_current_password";

        RegularMember regularMember = RegularMember.create("user@example.com", encodedCurrentPassword);
        ReflectionTestUtils.setField(regularMember, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));
        when(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(false);

        assertThrows(MemberException.class, () ->
                memberService.changePassword(memberId, currentPassword, newPassword));

        verify(memberRepository).findById(memberId);
        verify(passwordEncoder).matches(currentPassword, encodedCurrentPassword);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원의 비밀번호를 변경하려 하면 MemberException이 발생한다")
    void givenNonExistingMember_whenChangePassword_thenThrowsMemberException() {
        Long memberId = 999L;
        String currentPassword = "current_password";
        String newPassword = "new_password";

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () ->
                memberService.changePassword(memberId, currentPassword, newPassword));

        verify(memberRepository).findById(memberId);
        verify(memberRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateInstructorProfile
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사 프로필을 정상적으로 변경하면 save가 호출되고 UserResponseDTO를 반환한다")
    void givenValidInstructorMember_whenUpdateInstructorProfile_thenProfileIsUpdatedAndReturnsUserResponseDTO() {
        Long instructorId = 1L;
        String name = "수정된 이름";
        String profileImageUrl = "https://example.com/updated_image.jpg";
        String introduction = "수정된 소개";

        InstructorMember instructorMember = InstructorMember.create("instructor@example.com", "encoded_password",
                "원래 이름", null, null);
        ReflectionTestUtils.setField(instructorMember, "id", instructorId);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));
        when(memberRepository.save(any(InstructorMember.class))).thenReturn(instructorMember);

        UserResponseDTO result = memberService.updateInstructorProfile(instructorId, name, profileImageUrl, introduction);

        verify(memberRepository).findById(instructorId);
        verify(memberRepository).save(any(InstructorMember.class));

        assertEquals(instructorId, result.id());
        assertEquals("instructor@example.com", result.email());
        assertEquals(MemberRole.INSTRUCTOR, result.role());
    }

    @Test
    @DisplayName("일반 회원의 프로필을 변경하려 하면 MemberException이 발생하고 save가 호출되지 않는다")
    void givenRegularMember_whenUpdateInstructorProfile_thenThrowsMemberExceptionAndSaveIsNotCalled() {
        Long memberId = 1L;
        String name = "수정된 이름";
        String profileImageUrl = "https://example.com/updated_image.jpg";
        String introduction = "수정된 소개";

        RegularMember regularMember = RegularMember.create("user@example.com", "encoded_password");
        ReflectionTestUtils.setField(regularMember, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        assertThrows(MemberException.class, () ->
                memberService.updateInstructorProfile(memberId, name, profileImageUrl, introduction));

        verify(memberRepository).findById(memberId);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 강사의 프로필을 변경하려 하면 MemberException이 발생한다")
    void givenNonExistingInstructor_whenUpdateInstructorProfile_thenThrowsMemberException() {
        Long instructorId = 999L;
        String name = "수정된 이름";
        String profileImageUrl = "https://example.com/updated_image.jpg";
        String introduction = "수정된 소개";

        when(memberRepository.findById(instructorId)).thenReturn(Optional.empty());

        assertThrows(MemberException.class, () ->
                memberService.updateInstructorProfile(instructorId, name, profileImageUrl, introduction));

        verify(memberRepository).findById(instructorId);
        verify(memberRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // createFromHash (MEMBER-08: internal API for Auth service)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("이메일 중복이 없으면 이미 해시된 비밀번호를 재인코딩 없이 그대로 저장하고 memberId를 반환한다")
    void givenNonDuplicateEmailAndAlreadyHashedPassword_whenCreateFromHash_thenSavesHashAsIsAndReturnsMemberId() {
        String email = "auth-user@example.com";
        String passwordHash = "$2a$10$alreadyHashedValueFromAuth";

        RegularMember savedMember = RegularMember.create(email, passwordHash);
        ReflectionTestUtils.setField(savedMember, "id", 10L);

        when(memberRepository.existsByEmail(email)).thenReturn(false);
        when(memberRepository.save(any(RegularMember.class))).thenReturn(savedMember);

        CreateMemberResponse result = memberService.createFromHash(email, passwordHash);

        ArgumentCaptor<RegularMember> memberCaptor = ArgumentCaptor.forClass(RegularMember.class);
        verify(memberRepository).save(memberCaptor.capture());
        verifyNoInteractions(passwordEncoder);

        assertEquals(passwordHash, memberCaptor.getValue().getPassword());
        assertEquals(10L, result.memberId());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 해시 기반 회원 생성을 요청하면 MemberException이 발생하고 save와 인코딩이 호출되지 않는다")
    void givenExistingEmail_whenCreateFromHash_thenThrowsMemberExceptionAndSaveAndEncodeAreNotInvoked() {
        String email = "auth-user@example.com";
        String passwordHash = "$2a$10$alreadyHashedValueFromAuth";

        when(memberRepository.existsByEmail(email)).thenReturn(true);

        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.createFromHash(email, passwordHash));

        assertEquals("이미 사용 중인 이메일 입니다.", exception.getMessage());
        verify(memberRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // -------------------------------------------------------------------------
    // findByEmailForAuth (MEMBER-08: internal API for Auth login)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("존재하는 이메일로 조회하면 memberId, passwordHash, role, suspended, deleted를 담은 응답을 반환한다")
    void givenExistingEmail_whenFindByEmailForAuth_thenReturnsMemberCredentialResponse() {
        String email = "user@example.com";
        String passwordHash = "$2a$10$storedHashValue";

        RegularMember member = RegularMember.create(email, passwordHash);
        ReflectionTestUtils.setField(member, "id", 5L);

        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));

        MemberCredentialResponse result = memberService.findByEmailForAuth(email);

        verify(memberRepository).findByEmail(email);

        assertEquals(5L, result.memberId());
        assertEquals(passwordHash, result.passwordHash());
        assertEquals(MemberRole.MEMBER, result.role());
        assertFalse(result.suspended());
        assertFalse(result.deleted());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 MemberException이 발생한다")
    void givenNonExistingEmail_whenFindByEmailForAuth_thenThrowsMemberException() {
        String email = "missing@example.com";

        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.findByEmailForAuth(email));

        assertEquals("존재하지 않는 회원입니다.", exception.getMessage());
        verify(memberRepository).findByEmail(email);
    }

    // -------------------------------------------------------------------------
    // getAuthStatus (MEMBER-08: internal API for Auth token reissue)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("존재하는 강사 회원 ID로 조회하면 role, suspended, deleted를 담은 인증 상태 응답을 반환한다")
    void givenExistingInstructorMember_whenGetAuthStatus_thenReturnsMemberAuthStatusResponse() {
        Long memberId = 1L;
        InstructorMember instructorMember = InstructorMember.create(
                "instructor@example.com", "hash", "홍길동", null, null);
        ReflectionTestUtils.setField(instructorMember, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(instructorMember));

        MemberAuthStatusResponse result = memberService.getAuthStatus(memberId);

        verify(memberRepository).findById(memberId);

        assertEquals(MemberRole.INSTRUCTOR, result.role());
        assertFalse(result.suspended());
        assertFalse(result.deleted());
    }

    @Test
    @DisplayName("정지된 회원의 인증 상태를 조회하면 suspended와 deleted가 true로 반환된다")
    void givenSuspendedMember_whenGetAuthStatus_thenReturnsSuspendedAndDeletedTrue() {
        Long memberId = 2L;
        RegularMember regularMember = RegularMember.create("suspended@example.com", "hash");
        regularMember.suspend();
        ReflectionTestUtils.setField(regularMember, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        MemberAuthStatusResponse result = memberService.getAuthStatus(memberId);

        verify(memberRepository).findById(memberId);

        assertEquals(MemberRole.MEMBER, result.role());
        assertTrue(result.suspended());
        assertTrue(result.deleted());
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 인증 상태를 조회하면 MemberException이 발생한다")
    void givenNonExistingMember_whenGetAuthStatus_thenThrowsMemberException() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.getAuthStatus(memberId));

        assertEquals("존재하지 않는 회원입니다.", exception.getMessage());
        verify(memberRepository).findById(memberId);
    }

    // -------------------------------------------------------------------------
    // getSuspensionStatus (MEMBER-08: internal API for Course 2nd-line defense)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강사 회원의 정지 상태를 조회하면 suspended=true를 반환한다")
    void givenSuspendedInstructorMember_whenGetSuspensionStatus_thenReturnsSuspendedTrue() {
        Long instructorId = 1L;
        InstructorMember instructorMember = InstructorMember.create(
                "instructor@example.com", "hash", "홍길동", null, null);
        instructorMember.suspend();
        ReflectionTestUtils.setField(instructorMember, "id", instructorId);

        when(memberRepository.findById(instructorId)).thenReturn(Optional.of(instructorMember));

        SuspensionStatusResponse result = memberService.getSuspensionStatus(instructorId);

        verify(memberRepository).findById(instructorId);

        assertTrue(result.suspended());
    }

    @Test
    @DisplayName("강사가 아닌 일반 회원의 정지 상태를 조회해도 강사 여부 검증 없이 suspended 값을 반환한다")
    void givenNonInstructorRegularMember_whenGetSuspensionStatus_thenReturnsSuspendedStatusWithoutInstructorCheck() {
        Long memberId = 2L;
        RegularMember regularMember = RegularMember.create("user@example.com", "hash");
        ReflectionTestUtils.setField(regularMember, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(regularMember));

        SuspensionStatusResponse result = memberService.getSuspensionStatus(memberId);

        verify(memberRepository).findById(memberId);

        assertFalse(result.suspended());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 정지 상태를 조회하면 MemberException이 발생한다")
    void givenNonExistingId_whenGetSuspensionStatus_thenThrowsMemberException() {
        Long memberId = 999L;

        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.getSuspensionStatus(memberId));

        assertEquals("존재하지 않는 회원입니다.", exception.getMessage());
        verify(memberRepository).findById(memberId);
    }
}
