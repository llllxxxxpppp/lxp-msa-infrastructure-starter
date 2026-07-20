package com.lcs.auth.controller;

import com.lcs.auth.controller.dto.request.LoginRequestDTO;
import com.lcs.auth.controller.dto.response.NewAccessTokenDTO;
import com.lcs.auth.controller.dto.response.TokenResponseDTO;
import com.lcs.auth.service.AuthService;
import com.lcs.auth.service.RefreshService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshService refreshService;

    public AuthController(AuthService authService, RefreshService refreshService) {
        this.authService = authService;
        this.refreshService = refreshService;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        // @formatter:off
        return Map.of(
                "service", "auth-service",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
        // @formatter:on
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        TokenResponseDTO tokenResponseDTO =
                authService.login(requestDTO.email(), requestDTO.password());

        return ResponseEntity.ok(tokenResponseDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<NewAccessTokenDTO> refresh(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String newAccessToken = refreshService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(new NewAccessTokenDTO(newAccessToken));
    }
}
