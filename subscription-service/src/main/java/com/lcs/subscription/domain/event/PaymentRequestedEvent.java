package com.lcs.subscription.domain.event;

/**
 * 결제 요청 이벤트.
 *
 * <p>구독권 생성 시 유료 결제가 필요한 경우 발행되며, {@code PaymentAdapter}가 이 이벤트를
 * 구독하여 PG(stub) 결제를 시도하고 그 결과를 구독권/결제 요청에 반영한다.
 */
public class PaymentRequestedEvent extends BaseDomainEvent {

    private final Long subscriptionId;
    private final Long paymentId;

    public PaymentRequestedEvent(Long subscriptionId, Long paymentId) {
        super();
        this.subscriptionId = subscriptionId;
        this.paymentId = paymentId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
