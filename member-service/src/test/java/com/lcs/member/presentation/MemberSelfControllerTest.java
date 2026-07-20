package com.lcs.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.member.application.dto.request.ChangePasswordRequest;
import com.lcs.member.application.dto.request.UpdateInstructorProfileRequest;
import com.lcs.member.application.dto.response.UserResponseDTO;
import com.lcs.member.application.service.MemberService;
import com.lcs.member.domain.model.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gateway가 인증을 마친 뒤 신뢰 헤더({@code X-Member-Id})로 호출자를 식별해 전달한다고
 * 가정한다(Msa-Conversion-member.md §4.2). 헤더 검증 자체는 Gateway 책임이므로
 * 이 서비스 레벨 테스트는 헤더가 있을 때의 정상 흐름과, 헤더 누락 시 Spring이
 * 기본 제공하는 400 응답만 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class MemberSelfControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    // --- PATCH /api/members/me/password (changePassword) ---

    @Test
    @DisplayName("X-Member-Id 헤더를 포함해 비밀번호 변경을 요청하면 204 No Content를 반환한다")
    void givenMemberIdHeader_whenChangePassword_thenReturns204() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        mockMvc.perform(patch("/api/members/me/password")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(memberService).changePassword(1L, "current_password", "new_password");
    }

    @Test
    @DisplayName("X-Member-Id 헤더가 없으면 400 Bad Request를 반환하고 서비스는 호출되지 않는다")
    void givenNoMemberIdHeader_whenChangePassword_thenReturns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        mockMvc.perform(patch("/api/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(memberService);
    }

    // --- PATCH /api/members/me/instructor-profile (updateInstructorProfile) ---

    @Test
    @DisplayName("X-Member-Id 헤더를 포함해 프로필 변경을 요청하면 200 OK를 반환하고 UserResponseDTO를 반환한다")
    void givenMemberIdHeader_whenUpdateInstructorProfile_thenReturns200() throws Exception {
        UpdateInstructorProfileRequest request = new UpdateInstructorProfileRequest(
                "홍길동", "https://example.com/profile.jpg", "안녕하세요.");

        UserResponseDTO responseDTO = new UserResponseDTO(1L, "instructor@example.com", MemberRole.INSTRUCTOR);
        when(memberService.updateInstructorProfile(
                eq(1L),
                eq("홍길동"),
                eq("https://example.com/profile.jpg"),
                eq("안녕하세요.")
        )).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/members/me/instructor-profile")
                        .header(MEMBER_ID_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(memberService).updateInstructorProfile(
                1L,
                "홍길동",
                "https://example.com/profile.jpg",
                "안녕하세요.");
    }

    // --- DELETE /api/members/me (withdrawMember) ---

    @Test
    @DisplayName("X-Member-Id 헤더를 포함해 탈퇴를 요청하면 204 No Content를 반환한다")
    void givenMemberIdHeader_whenWithdraw_thenReturns204() throws Exception {
        mockMvc.perform(delete("/api/members/me")
                        .header(MEMBER_ID_HEADER, "1"))
                .andExpect(status().isNoContent());

        verify(memberService).withdrawMember(1L);
    }
}
