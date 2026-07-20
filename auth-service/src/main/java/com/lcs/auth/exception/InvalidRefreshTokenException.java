package com.lcs.auth.exception;

import java.io.Serial;

public class InvalidRefreshTokenException extends AuthException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
