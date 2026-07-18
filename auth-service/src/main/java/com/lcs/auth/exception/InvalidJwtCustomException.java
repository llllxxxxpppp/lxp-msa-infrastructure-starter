package com.lcs.auth.exception;

import java.io.Serial;

public class InvalidJwtCustomException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidJwtCustomException(String message) {
        super(message);
    }

    public InvalidJwtCustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
