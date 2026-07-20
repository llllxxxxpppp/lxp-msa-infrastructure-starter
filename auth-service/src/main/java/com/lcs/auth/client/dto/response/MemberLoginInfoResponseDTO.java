package com.lcs.auth.client.dto.response;

public record MemberLoginInfoResponseDTO(
        Long id,
        String password,
        String role,
        boolean suspended,
        boolean deleted
) {
}
