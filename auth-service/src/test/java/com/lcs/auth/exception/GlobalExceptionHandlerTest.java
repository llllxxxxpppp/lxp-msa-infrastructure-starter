package com.lcs.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    @DisplayName("MethodArgumentNotValidException은 400 Bad Request와 필드 검증 메시지로 변환된다")
    void handleMethodArgumentNotValidException_returns400WithFieldErrorMessages() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "loginRequestDTO");
        bindingResult.addError(
                new FieldError("loginRequestDTO", "email", "이메일은 필수 입력 항목입니다."));
        bindingResult.addError(
                new FieldError("loginRequestDTO", "password", "비밀번호는 6자 이상 100자 이하로 입력해야 합니다."));

        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyLoginMethod", Object.class), 0);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response =
                handler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("이메일은 필수 입력 항목입니다., 비밀번호는 6자 이상 100자 이하로 입력해야 합니다.");
    }

    @SuppressWarnings("unused")
    private void dummyLoginMethod(Object requestDTO) {
    }
}
