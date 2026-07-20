package com.lcs.auth.exception;

import java.io.Serial;

public class ExpiredJwtCustomException extends AuthException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExpiredJwtCustomException(String message) {
        super(message);
    }

    public ExpiredJwtCustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
