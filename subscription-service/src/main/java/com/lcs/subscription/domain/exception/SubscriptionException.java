package com.lcs.subscription.domain.exception;

public class SubscriptionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SubscriptionException(String message) {
        super(message);
    }
}
