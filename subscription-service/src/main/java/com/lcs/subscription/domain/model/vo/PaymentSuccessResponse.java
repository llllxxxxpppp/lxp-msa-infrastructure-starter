package com.lcs.subscription.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.Objects;

public record PaymentSuccessResponse(PaymentId paymentId, OffsetDateTime approvedAt) {

    public PaymentSuccessResponse {
        Objects.requireNonNull(paymentId, "PaymentId는 null일 수 없습니다.");
        Objects.requireNonNull(approvedAt, "승인 일시는 null일 수 없습니다.");
    }
}
