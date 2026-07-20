package com.lcs.subscription.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.Objects;

public record RefundSuccessResponse(PaymentId paymentId, OffsetDateTime refundedAt) {

    public RefundSuccessResponse {
        Objects.requireNonNull(paymentId, "PaymentId는 null일 수 없습니다.");
        Objects.requireNonNull(refundedAt, "환불 일시는 null일 수 없습니다.");
    }
}
