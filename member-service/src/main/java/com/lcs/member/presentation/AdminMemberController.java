package com.lcs.member.presentation;

import com.lcs.member.application.dto.request.RegisterInstructorRequest;
import com.lcs.member.application.dto.response.UserResponseDTO;
import com.lcs.member.application.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberController {
    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/instructors")
    public ResponseEntity<UserResponseDTO> registerInstructor(
            @RequestBody @Valid RegisterInstructorRequest requestDTO
    ) {
        UserResponseDTO responseDTO = memberService.registerInstructor(
                requestDTO.email(),
                requestDTO.password(),
                requestDTO.name(),
                requestDTO.profileImageUrl(),
                requestDTO.introduction());

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PostMapping("/instructors/{instructorId}/suspend")
    public ResponseEntity<Void> suspendInstructor(@PathVariable Long instructorId) {
        memberService.suspendInstructor(instructorId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{memberId}/suspend")
    public ResponseEntity<Void> suspendMember(@PathVariable Long memberId) {
        memberService.suspendMember(memberId);

        return ResponseEntity.ok().build();
    }
}
