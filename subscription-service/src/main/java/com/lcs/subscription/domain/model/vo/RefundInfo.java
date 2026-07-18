package com.lcs.subscription.domain.model.vo;

import com.lcs.subscription.domain.exception.SubscriptionException;
import java.util.Objects;

public record RefundInfo(PaymentId paymentId, int amount) {

    public RefundInfo {
        Objects.requireNonNull(paymentId, "PaymentId는 null일 수 없습니다.");
        if (amount <= 0) {
            throw new SubscriptionException("환불 금액은 0보다 커야 합니다.");
        }
    }
}
