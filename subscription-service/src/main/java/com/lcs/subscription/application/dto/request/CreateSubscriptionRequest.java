package com.lcs.subscription.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSubscriptionRequest(
        @NotNull
        @Positive
        Long memberId
) {
}
