package com.lcs.subscription.domain.model.vo;

import com.lcs.subscription.domain.exception.SubscriptionException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentInfo {

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false)
    private int amount;

    protected PaymentInfo() {}

    public PaymentInfo(String idempotencyKey, int amount) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new SubscriptionException("멱등키는 비어있을 수 없습니다.");
        }
        if (amount < 0) {
            throw new SubscriptionException("결제 금액은 0 이상이어야 합니다.");
        }
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isFree() {
        return amount == 0;
    }
}
