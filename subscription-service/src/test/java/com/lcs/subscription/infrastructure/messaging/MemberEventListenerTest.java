package com.lcs.subscription.infrastructure.messaging;

import com.lcs.subscription.application.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MemberEventListenerTest {

    private SubscriptionService subscriptionService;
    private MemberEventListener listener;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        listener = new MemberEventListener(subscriptionService);
    }

    @Test
    void registeredEventCreatesFreeSubscription() {
        MemberEventMessage event = event(1L);

        listener.handle(event, "member.registered");

        verify(subscriptionService).createFreeSubscriptionIfAbsent(1L);
    }

    @Test
    void suspendedAndWithdrawnEventsSuspendSubscriptions() {
        MemberEventMessage event = event(1L);

        listener.handle(event, "member.suspended");
        listener.handle(event, "member.withdrawn");

        verify(subscriptionService, org.mockito.Mockito.times(2))
                .suspendActiveSubscriptions(1L);
    }

    @Test
    void unsupportedEventIsIgnored() {
        listener.handle(event(1L), "unknown.event");

        verifyNoInteractions(subscriptionService);
    }

    private MemberEventMessage event(Long memberId) {
        return new MemberEventMessage(UUID.randomUUID(), OffsetDateTime.now(), memberId);
    }
}
