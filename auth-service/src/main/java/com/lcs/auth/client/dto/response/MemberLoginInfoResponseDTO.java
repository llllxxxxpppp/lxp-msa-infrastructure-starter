package com.lcs.auth.client.dto.response;

public record MemberLoginInfoResponseDTO(
        Long memberId,
        String passwordHash,
        String role,
        boolean suspended,
        boolean deleted
) {
}
