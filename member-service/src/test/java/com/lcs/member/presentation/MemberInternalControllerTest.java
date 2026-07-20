package com.lcs.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.member.application.dto.request.CreateMemberRequest;
import com.lcs.member.application.dto.request.EmailLookupRequest;
import com.lcs.member.application.dto.response.CreateMemberResponse;
import com.lcs.member.application.dto.response.MemberAuthStatusResponse;
import com.lcs.member.application.dto.response.MemberCredentialResponse;
import com.lcs.member.application.dto.response.SuspensionStatusResponse;
import com.lcs.member.application.service.MemberService;
import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MEMBER-08: Auth/Course 서비스가 호출할 내부 전용 API 4종.
 * 이 컨트롤러는 Gateway가 외부에 노출하지 않는 내부망 전용 엔드포인트이므로
 * 인증/인가(401/403) 시나리오는 검증 대상이 아니다(Msa-Conversion-member.md §3.1, §4.3).
 * 역할(role) 표현은 이 세션에서 단일 {@code role} 필드(MemberRole)로 확정했다 —
 * 복수형 {@code roles}가 아니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class MemberInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    // --- POST /internal/members (createFromHash) ---

    @Test
    @DisplayName("이미 해시된 비밀번호로 회원 생성을 요청하면 201 Created와 memberId를 반환한다")
    void givenEmailAndPasswordHash_whenCreateMember_thenReturns201WithMemberId() throws Exception {
        CreateMemberRequest request = new CreateMemberRequest("auth-user@example.com", "$2a$10$alreadyHashedValue");

        when(memberService.createFromHash("auth-user@example.com", "$2a$10$alreadyHashedValue"))
                .thenReturn(new CreateMemberResponse(1L));

        mockMvc.perform(post("/internal/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(1));

        verify(memberService).createFromHash("auth-user@example.com", "$2a$10$alreadyHashedValue");
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로 회원 생성을 요청하면 400 Bad Request를 반환한다")
    void givenDuplicateEmail_whenCreateMember_thenReturns400() throws Exception {
        CreateMemberRequest request = new CreateMemberRequest("dup@example.com", "$2a$10$alreadyHashedValue");

        when(memberService.createFromHash("dup@example.com", "$2a$10$alreadyHashedValue"))
                .thenThrow(new MemberException("이미 사용 중인 이메일 입니다."));

        mockMvc.perform(post("/internal/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일 입니다."));

        verify(memberService).createFromHash("dup@example.com", "$2a$10$alreadyHashedValue");
    }

    // --- POST /internal/members/by-email/info (findByEmailForAuth) ---

    @Test
    @DisplayName("존재하는 이메일로 조회하면 200 OK와 memberId, passwordHash, role, suspended, deleted를 반환한다")
    void givenExistingEmail_whenFindByEmail_thenReturns200WithCredentialFields() throws Exception {
        String email = "user@example.com";
        EmailLookupRequest request = new EmailLookupRequest(email);
        MemberCredentialResponse response =
                new MemberCredentialResponse(1L, "$2a$10$storedHash", MemberRole.MEMBER, false, false);

        when(memberService.findByEmailForAuth(email)).thenReturn(response);

        mockMvc.perform(post("/internal/members/by-email/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.passwordHash").value("$2a$10$storedHash"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.suspended").value(false))
                .andExpect(jsonPath("$.deleted").value(false));

        verify(memberService).findByEmailForAuth(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 400 Bad Request를 반환한다")
    void givenNonExistingEmail_whenFindByEmail_thenReturns400() throws Exception {
        String email = "missing@example.com";
        EmailLookupRequest request = new EmailLookupRequest(email);

        when(memberService.findByEmailForAuth(email))
                .thenThrow(new MemberException("존재하지 않는 회원입니다."));

        mockMvc.perform(post("/internal/members/by-email/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."));

        verify(memberService).findByEmailForAuth(email);
    }

    // --- GET /internal/members/{memberId}/auth-status (getAuthStatus) ---

    @Test
    @DisplayName("존재하는 회원 ID로 인증 상태를 조회하면 200 OK와 role, suspended, deleted를 반환한다")
    void givenExistingMemberId_whenGetAuthStatus_thenReturns200WithAuthStatusFields() throws Exception {
        MemberAuthStatusResponse response = new MemberAuthStatusResponse(MemberRole.INSTRUCTOR, false, false);

        when(memberService.getAuthStatus(1L)).thenReturn(response);

        mockMvc.perform(get("/internal/members/{memberId}/auth-status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.suspended").value(false))
                .andExpect(jsonPath("$.deleted").value(false));

        verify(memberService).getAuthStatus(1L);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 인증 상태를 조회하면 400 Bad Request를 반환한다")
    void givenNonExistingMemberId_whenGetAuthStatus_thenReturns400() throws Exception {
        when(memberService.getAuthStatus(999L))
                .thenThrow(new MemberException("존재하지 않는 회원입니다."));

        mockMvc.perform(get("/internal/members/{memberId}/auth-status", 999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."));

        verify(memberService).getAuthStatus(999L);
    }

    // --- GET /internal/members/{instructorId}/suspension-status (getSuspensionStatus) ---

    @Test
    @DisplayName("존재하는 ID로 정지 상태를 조회하면 200 OK와 suspended 값을 반환한다")
    void givenExistingId_whenGetSuspensionStatus_thenReturns200WithSuspendedField() throws Exception {
        when(memberService.getSuspensionStatus(1L)).thenReturn(new SuspensionStatusResponse(true));

        mockMvc.perform(get("/internal/members/{instructorId}/suspension-status", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true));

        verify(memberService).getSuspensionStatus(1L);
    }

    @Test
    @DisplayName("강사가 아닌 일반 회원 ID로 정지 상태를 조회해도 200 OK와 suspended 값을 반환한다")
    void givenNonInstructorMemberId_whenGetSuspensionStatus_thenReturns200WithSuspendedField() throws Exception {
        when(memberService.getSuspensionStatus(2L)).thenReturn(new SuspensionStatusResponse(false));

        mockMvc.perform(get("/internal/members/{instructorId}/suspension-status", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(false));

        verify(memberService).getSuspensionStatus(2L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 정지 상태를 조회하면 400 Bad Request를 반환한다")
    void givenNonExistingId_whenGetSuspensionStatus_thenReturns400() throws Exception {
        when(memberService.getSuspensionStatus(999L))
                .thenThrow(new MemberException("존재하지 않는 회원입니다."));

        mockMvc.perform(get("/internal/members/{instructorId}/suspension-status", 999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."));

        verify(memberService).getSuspensionStatus(999L);
    }
}
