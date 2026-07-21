package com.lcs.subscription.infrastructure.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberEventMessage(
        UUID eventId,
        OffsetDateTime occurredAt,
        Long memberId
) {
}
