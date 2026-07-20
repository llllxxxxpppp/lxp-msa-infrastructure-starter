package com.lcs.member.domain.exception;

public class MemberException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MemberException(String message) {
        super(message);
    }
}
