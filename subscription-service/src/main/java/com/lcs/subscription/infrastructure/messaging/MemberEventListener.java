package com.lcs.subscription.infrastructure.messaging;

import com.lcs.subscription.application.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class MemberEventListener {

    private static final Logger log = LoggerFactory.getLogger(MemberEventListener.class);

    private final SubscriptionService subscriptionService;

    public MemberEventListener(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @RabbitListener(queues = RabbitMqConfig.SUBSCRIPTION_MEMBER_QUEUE)
    public void handle(
            MemberEventMessage event,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) {
        log.info("Received member event. routingKey={}, memberId={}", routingKey, event.memberId());

        switch (routingKey) {
            case "member.registered" ->
                    subscriptionService.createFreeSubscriptionIfAbsent(event.memberId());
            case "member.suspended" ->
                    subscriptionService.suspendActiveSubscriptions(event.memberId());
            case "member.withdrawn" ->
                    subscriptionService.processMemberWithdrawal(event.memberId());
            default -> log.warn("Unsupported member event routing key: {}", routingKey);
        }
    }
}
