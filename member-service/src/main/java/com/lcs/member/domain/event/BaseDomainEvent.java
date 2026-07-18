package com.lcs.member.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BaseDomainEvent {

    private final UUID eventId;
    private final OffsetDateTime occurredAt;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = OffsetDateTime.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
