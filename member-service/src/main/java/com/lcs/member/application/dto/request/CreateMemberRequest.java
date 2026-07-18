package com.lcs.member.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "유효한 이메일 주소를 입력해야 합니다.")
        String email,

        @NotBlank(message = "비밀번호 해시는 필수 입력 항목입니다.")
        String passwordHash
) {
}
