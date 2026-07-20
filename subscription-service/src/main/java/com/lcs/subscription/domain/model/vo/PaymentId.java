package com.lcs.subscription.domain.model.vo;

import java.util.Objects;

public record PaymentId(Long value) {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId는 null일 수 없습니다.");
    }
}
