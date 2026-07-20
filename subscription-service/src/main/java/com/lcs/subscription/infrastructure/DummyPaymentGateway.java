package com.lcs.subscription.infrastructure;

import org.springframework.stereotype.Component;

@Component
public class DummyPaymentGateway implements PaymentGateway {

    @Override
    public boolean pay(String idempotencyKey, int amount) {
        return true;
    }

    @Override
    public boolean refund(String idempotencyKey) {
        return true;
    }
}
