package com.lcs.member.presentation;

import com.lcs.member.application.dto.request.ChangePasswordRequest;
import com.lcs.member.application.dto.request.UpdateInstructorProfileRequest;
import com.lcs.member.application.dto.response.UserResponseDTO;
import com.lcs.member.application.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway가 검증 후 전달하는 {@code X-User-Id} 신뢰 헤더로 호출자를 식별한다
 * (Msa-Conversion-member.md §4.2 확정 사항 — JWT/Authentication 캐스팅 방식 폐기).
 */
@Tag(name = "회원 자기 관리", description = "회원 본인이 비밀번호 변경/강사 프로필 수정/탈퇴를 처리하는 API")
@RestController
@RequestMapping("/api/members/me")
public class MemberSelfController {

    private final MemberService memberService;

    public MemberSelfController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") Long memberId,
            @RequestBody @Valid ChangePasswordRequest request) {
        memberService.changePassword(memberId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/instructor-profile")
    public ResponseEntity<UserResponseDTO> updateInstructorProfile(
            @RequestHeader("X-User-Id") Long memberId,
            @RequestBody @Valid UpdateInstructorProfileRequest request) {
        UserResponseDTO response = memberService.updateInstructorProfile(
                memberId, request.name(), request.profileImageUrl(), request.introduction());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(@RequestHeader("X-User-Id") Long memberId) {
        memberService.withdrawMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
