package com.lcs.gateway.jwt.exception;

import java.io.Serial;

/**
 * JWT 서명이 유효하지 않거나 형식이 잘못된 경우 등
 * 만료 외의 모든 검증 실패한 경우
 */
public class InvalidTokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
