package com.lcs.auth.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MemberServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleMemberServiceUnavailableException(
            MemberServiceUnavailableException e
    ) {
        return memberServiceUnavailableResponse();
    }

    // 회로 OPEN 상태에서 member-service 호출이 차단되면 발생. fail-fast로 즉시 503을 반환한다.
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermittedException(CallNotPermittedException e) {
        return memberServiceUnavailableResponse();
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ErrorResponse> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException e
    ) {
        // Security가 loadUserByUsername 중 발생한 예외를 감싸므로 원인을 언랩해 503으로 매핑한다.
        Throwable cause = e.getCause();
        if (cause instanceof MemberServiceUnavailableException
                || cause instanceof CallNotPermittedException) {
            return memberServiceUnavailableResponse();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 내부 오류가 발생했습니다."));
    }

    private ResponseEntity<ErrorResponse> memberServiceUnavailableResponse() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("회원 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
