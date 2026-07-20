package com.lcs.auth.exception;

import java.io.Serial;

public class MemberServiceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MemberServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
