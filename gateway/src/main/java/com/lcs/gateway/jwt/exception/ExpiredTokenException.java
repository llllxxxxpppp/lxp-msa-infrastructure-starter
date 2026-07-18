package com.lcs.gateway.jwt.exception;

import java.io.Serial;

/**
 * JWT의 유효기간이 만료된 경우
 * Gateway는 재발급을 시도하지 않고 401로 응답한다.
 */
public class ExpiredTokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
