package com.lcs.auth.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "유효한 이메일 주소를 입력해야 합니다.")
        @Size(max = 50, message = "이메일은 50자 이하로 입력해야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Size(min = 6, max = 100, message = "비밀번호는 6자 이상 100자 이하로 입력해야 합니다.")
        String password
) {
}
