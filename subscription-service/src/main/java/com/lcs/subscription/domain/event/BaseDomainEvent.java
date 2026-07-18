package com.lcs.subscription.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class BaseDomainEvent {

    private final UUID eventId = UUID.randomUUID();
    private final OffsetDateTime occurredAt = OffsetDateTime.now();

    public UUID getEventId() {
        return eventId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
