package com.lcs.subscription.domain.model.vo;

import java.time.OffsetDateTime;
import java.util.Objects;

public record RefundFailureResponse(PaymentId paymentId, String reason, OffsetDateTime failedAt) {

    public RefundFailureResponse {
        Objects.requireNonNull(paymentId, "PaymentId는 null일 수 없습니다.");
        Objects.requireNonNull(failedAt, "실패 일시는 null일 수 없습니다.");
    }
}
