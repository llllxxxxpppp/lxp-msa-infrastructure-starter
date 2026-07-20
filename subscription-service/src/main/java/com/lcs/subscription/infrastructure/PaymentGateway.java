package com.lcs.subscription.infrastructure;

public interface PaymentGateway {

    boolean pay(String idempotencyKey, int amount);

    boolean refund(String idempotencyKey);
}
