package com.lcs.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("AuthException은 400 Bad Request와 메시지로 변환된다")
    void handleAuthException_returns400WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAuthException(new InvalidRefreshTokenException("Refresh token expired"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Refresh token expired");
    }

    @Test
    @DisplayName("AuthenticationException은 401 Unauthorized와 메시지로 변환된다")
    void handleAuthenticationException_returns401WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAuthenticationException(new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bad credentials");
    }
}
