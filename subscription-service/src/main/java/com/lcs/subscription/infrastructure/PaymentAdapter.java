package com.lcs.subscription.infrastructure;

import com.lcs.subscription.domain.event.PaymentRequestedEvent;
import com.lcs.subscription.domain.event.RefundRequestedEvent;
import com.lcs.subscription.domain.exception.SubscriptionException;
import com.lcs.subscription.domain.model.entity.Payment;
import com.lcs.subscription.domain.model.entity.Subscription;
import com.lcs.subscription.domain.model.vo.ResponseResult;
import com.lcs.subscription.domain.repository.PaymentRepository;
import com.lcs.subscription.domain.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 결제/환불 요청 이벤트 리스너.
 *
 * <p>{@code SubscriptionService}가 더 이상 PG(stub)를 직접 호출하지 않고 결제/환불 요청
 * 이벤트를 발행하면, 이 어댑터가 이벤트를 구독하여 PG를 호출하고 그 결과를 같은 메서드
 * 안에서 동기적으로 구독권/결제 요청 애그리거트에 반영·저장한다.
 *
 * <p>Payment는 Subscription 애그리거트의 자식 엔티티이므로, 이 어댑터는 Payment를
 * 직접 변경하지 않고 항상 Subscription 루트가 제공하는 메서드
 * (markPaymentRequested/markPaymentResponded)를 통해서만 상태를 변경한다.
 */
@Component
public class PaymentAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentAdapter.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentAdapter(
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    @EventListener
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        logPaymentRequestedEvent("Handling event", event);

        Subscription subscription = findSubscription(event.getSubscriptionId());
        Payment payment = findPayment(event.getPaymentId());

        subscription.markPaymentRequested(event.getPaymentId());
        boolean success = paymentGateway.pay(idempotencyKeyOf(payment), amountOf(subscription));

        if (success) {
            subscription.markPaymentResponded(event.getPaymentId(), ResponseResult.SUCCESS);
            subscription.activate();
            subscriptionRepository.save(subscription);
        } else {
            subscription.markPaymentResponded(event.getPaymentId(), ResponseResult.FAILED);
        }
        paymentRepository.save(payment);

        logPaymentRequestedEvent("Handled event", event);
    }

    @EventListener
    public void handleRefundRequested(RefundRequestedEvent event) {
        logRefundRequestedEvent("Handling event", event);

        Subscription subscription = findSubscription(event.getSubscriptionId());
        Payment payment = findPayment(event.getPaymentId());

        subscription.markPaymentRequested(event.getPaymentId());
        boolean success = paymentGateway.refund(idempotencyKeyOf(payment));

        if (success) {
            subscription.markPaymentResponded(event.getPaymentId(), ResponseResult.SUCCESS);
            subscription.suspend();
            subscriptionRepository.save(subscription);
        } else {
            subscription.markPaymentResponded(event.getPaymentId(), ResponseResult.FAILED);
        }
        paymentRepository.save(payment);

        logRefundRequestedEvent("Handled event", event);
    }

    private String idempotencyKeyOf(Payment payment) {
        return payment.getRequestId().value().toString();
    }

    private int amountOf(Subscription subscription) {
        return subscription.getPrice().intValue();
    }

    private Subscription findSubscription(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException("구독권을 찾을 수 없습니다."));
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new SubscriptionException("결제 요청을 찾을 수 없습니다."));
    }

    private void logPaymentRequestedEvent(String phase, PaymentRequestedEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{}: type={}, eventId={}, occurredAt={}, subscriptionId={}, paymentId={}",
                    phase,
                    event.getClass().getSimpleName(),
                    event.getEventId(),
                    event.getOccurredAt(),
                    event.getSubscriptionId(),
                    event.getPaymentId());
        }
    }

    private void logRefundRequestedEvent(String phase, RefundRequestedEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{}: type={}, eventId={}, occurredAt={}, subscriptionId={}, paymentId={}",
                    phase,
                    event.getClass().getSimpleName(),
                    event.getEventId(),
                    event.getOccurredAt(),
                    event.getSubscriptionId(),
                    event.getPaymentId());
        }
    }
}
