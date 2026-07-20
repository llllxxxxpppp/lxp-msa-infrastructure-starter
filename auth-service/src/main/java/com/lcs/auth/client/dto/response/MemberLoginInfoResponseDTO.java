package com.lcs.auth.client.dto.response;

public record MemberLoginInfoResponseDTO(
        Long id,
        String email,
        String password,
        boolean deleted,
        String role
) {
}
