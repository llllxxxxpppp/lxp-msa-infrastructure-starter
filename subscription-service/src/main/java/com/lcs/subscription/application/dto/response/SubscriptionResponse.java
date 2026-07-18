package com.lcs.subscription.application.dto.response;

import com.lcs.subscription.domain.model.entity.Subscription;
import java.time.OffsetDateTime;

public record SubscriptionResponse(
        Long subscriptionId,
        Long memberId,
        Long parentId,
        Long generation,
        OffsetDateTime subscriptionStartAt,
        OffsetDateTime validUntil,
        OffsetDateTime activatedAt,
        OffsetDateTime suspendedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt
) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getMemberId(),
                subscription.getParentId(),
                subscription.getGeneration(),
                subscription.getSubscriptionStartAt(),
                subscription.getValidUntil(),
                subscription.getActivatedAt(),
                subscription.getSuspendedAt(),
                subscription.getCancelledAt(),
                subscription.getCreatedAt()
        );
    }
}
