package com.lcs.member.presentation;

import com.lcs.member.application.dto.request.CreateMemberRequest;
import com.lcs.member.application.dto.response.CreateMemberResponse;
import com.lcs.member.application.dto.response.MemberAuthStatusResponse;
import com.lcs.member.application.dto.response.MemberCredentialResponse;
import com.lcs.member.application.dto.response.SuspensionStatusResponse;
import com.lcs.member.application.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth/Course 서비스가 호출하는 내부 전용 API.
 * 인증/인가는 이 서비스의 책임이 아니며, Gateway가 외부 노출을 차단한다고 가정한다
 * (Msa-Conversion-member.md §3.1, §3.2, §4.3).
 */
@RestController
@RequestMapping("/internal/members")
public class MemberInternalController {
    private final MemberService memberService;

    public MemberInternalController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<CreateMemberResponse> createMember(
            @RequestBody @Valid CreateMemberRequest requestDTO
    ) {
        CreateMemberResponse responseDTO =
                memberService.createFromHash(requestDTO.email(), requestDTO.passwordHash());

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<MemberCredentialResponse> findByEmail(@PathVariable String email) {
        MemberCredentialResponse responseDTO = memberService.findByEmailForAuth(email);

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{memberId}/auth-status")
    public ResponseEntity<MemberAuthStatusResponse> getAuthStatus(@PathVariable Long memberId) {
        MemberAuthStatusResponse responseDTO = memberService.getAuthStatus(memberId);

        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{instructorId}/suspension-status")
    public ResponseEntity<SuspensionStatusResponse> getSuspensionStatus(@PathVariable Long instructorId) {
        SuspensionStatusResponse responseDTO = memberService.getSuspensionStatus(instructorId);

        return ResponseEntity.ok(responseDTO);
    }
}
