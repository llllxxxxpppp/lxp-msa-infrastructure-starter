package com.lcs.auth.client.dto.response;

public record MemberLoginInfoResponseDTO(
        Long id,
        String passwordHash,
        String role,
        boolean suspended,
        boolean deleted
) {
}
