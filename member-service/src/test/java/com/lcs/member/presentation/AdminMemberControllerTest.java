package com.lcs.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.member.application.dto.request.RegisterInstructorRequest;
import com.lcs.member.application.dto.response.UserResponseDTO;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 역할 기반 인가(관리자만 호출 가능 등)는 Gateway가 라우팅 단계에서 수행하므로
 * (Msa-Conversion-member.md §2), 이 서비스 레벨 테스트는 정상 요청 흐름만 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AdminMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    // --- POST /api/admin/members/instructors (registerInstructor) ---

    @Test
    @DisplayName("강사 등록을 요청하면 201 Created를 반환한다")
    void givenValidRequest_whenRegisterInstructor_thenReturns201() throws Exception {
        RegisterInstructorRequest request = new RegisterInstructorRequest(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );

        UserResponseDTO mockResponse = new UserResponseDTO(1L, "instructor@example.com", MemberRole.INSTRUCTOR);
        when(memberService.registerInstructor(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/api/admin/members/instructors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(memberService).registerInstructor(
                "instructor@example.com",
                "password123",
                "홍길동",
                "https://example.com/image.jpg",
                "안녕하세요"
        );
    }

    // --- POST /api/admin/members/instructors/{instructorId}/suspend (suspendInstructor) ---

    @Test
    @DisplayName("강사 정지를 요청하면 200 OK를 반환한다")
    void givenValidInstructorId_whenSuspendInstructor_thenReturns200() throws Exception {
        mockMvc.perform(post("/api/admin/members/instructors/1/suspend"))
                .andExpect(status().isOk());

        verify(memberService).suspendInstructor(1L);
    }

    // --- POST /api/admin/members/{memberId}/suspend (suspendMember) ---

    @Test
    @DisplayName("일반 회원 정지를 요청하면 200 OK를 반환한다")
    void givenValidMemberId_whenSuspendMember_thenReturns200() throws Exception {
        doNothing().when(memberService).suspendMember(1L);

        mockMvc.perform(post("/api/admin/members/1/suspend"))
                .andExpect(status().isOk());

        verify(memberService).suspendMember(1L);
    }

    @Test
    @DisplayName("강사 ID로 일반 회원 정지를 요청하면 서비스가 던진 예외로 400 Bad Request를 반환한다")
    void givenInstructorId_whenSuspendMember_thenReturns400() throws Exception {
        doThrow(new MemberException("일반 회원이 아닙니다."))
                .when(memberService).suspendMember(2L);

        mockMvc.perform(post("/api/admin/members/2/suspend"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("일반 회원이 아닙니다."));

        verify(memberService).suspendMember(2L);
    }
}
