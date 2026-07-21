package com.lcs.subscription.infrastructure.messaging;

import com.lcs.subscription.domain.model.entity.Subscription;
import com.lcs.subscription.domain.model.vo.RequestType;
import com.lcs.subscription.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SUB-05: member-service가 실제 RabbitMQ 브로커로 발행하는 {@code member.events} 메시지를
 * subscription-service가 실제로 수신·역직렬화해 {@code SubscriptionService}에 반영하는지
 * 확인하는 end-to-end 통합 테스트.
 *
 * <p><b>사전 준비</b>: 이 테스트를 실행하려면 로컬에서 실제 RabbitMQ 브로커가 떠 있어야 한다
 * (저장소 루트에서 {@code docker compose up -d rabbitmq}). {@code rabbitmq-integration} 태그가
 * 붙어 있어 기본 {@code ./gradlew test}/{@code check}에서는 제외된다.</p>
 */
@Tag("rabbitmq-integration")
@SpringBootTest
class RabbitMqEventConsumingIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 10_000L;
    private static final long POLL_INTERVAL_MILLIS = 200L;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange memberEventsExchange;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void registeredEventCreatesFreeSubscriptionFromRealBroker() {
        Long memberId = newMemberId();

        publish("member.registered", memberId);

        boolean created = awaitUntil(() -> subscriptionRepository.existsByMemberId(memberId));

        assertTrue(created, "Expected a free subscription to be created for memberId=" + memberId
                + " within " + POLL_TIMEOUT_MILLIS + "ms. Is a real RabbitMQ broker running "
                + "(docker compose up -d rabbitmq)?");
    }

    @Test
    void suspendedEventSuspendsActiveSubscriptionFromRealBroker() {
        Long memberId = newMemberId();
        Subscription subscription = Subscription.create(memberId, 0L);
        subscription.activate();
        subscriptionRepository.save(subscription);

        publish("member.suspended", memberId);

        boolean suspended = awaitUntil(() -> findSuspendedAt(memberId).isPresent());

        assertTrue(suspended, "Expected the subscription for memberId=" + memberId
                + " to be suspended within " + POLL_TIMEOUT_MILLIS + "ms.");
    }

    @Test
    @Transactional
    void withdrawnEventRequestsRefundInsteadOfSuspendingRefundEligibleSubscriptionFromRealBroker() {
        Long memberId = newMemberId();
        Subscription subscription = Subscription.create(memberId, 9_900L);
        subscription.activate();
        subscriptionRepository.save(subscription);

        publish("member.withdrawn", memberId);

        boolean refundRequested = awaitUntil(() -> hasRefundPayment(memberId));

        assertTrue(refundRequested, "Expected a REFUND payment to be requested for memberId=" + memberId
                + " within " + POLL_TIMEOUT_MILLIS + "ms.");
        assertFalse(findSuspendedAt(memberId).isPresent(),
                "Refund-eligible subscription must not be suspended directly on member.withdrawn "
                        + "(that would be the pre-SUB-03-fix behavior).");
    }

    private void publish(String routingKey, Long memberId) {
        MemberEventMessage event = new MemberEventMessage(UUID.randomUUID(), OffsetDateTime.now(), memberId);
        rabbitTemplate.convertAndSend(memberEventsExchange.getName(), routingKey, event);
    }

    private Long newMemberId() {
        return System.nanoTime();
    }

    private boolean awaitUntil(Supplier<Boolean> condition) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.get())) {
                return true;
            }
            sleep();
        }
        return Boolean.TRUE.equals(condition.get());
    }

    private void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Optional<OffsetDateTime> findSuspendedAt(Long memberId) {
        return subscriptionRepository.findByMemberId(memberId).stream()
                .findFirst()
                .map(Subscription::getSuspendedAt);
    }

    private boolean hasRefundPayment(Long memberId) {
        List<Subscription> subscriptions = subscriptionRepository.findByMemberId(memberId);
        return subscriptions.stream()
                .anyMatch(subscription -> subscription.getPayments().stream()
                        .anyMatch(payment -> payment.getRequestType() == RequestType.REFUND));
    }
}
