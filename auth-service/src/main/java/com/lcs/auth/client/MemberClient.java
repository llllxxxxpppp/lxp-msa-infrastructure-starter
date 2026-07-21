package com.lcs.auth.client;

import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import java.util.Optional;

public interface MemberClient {
    Optional<MemberLoginInfoResponseDTO> findByEmail(String email);
}
