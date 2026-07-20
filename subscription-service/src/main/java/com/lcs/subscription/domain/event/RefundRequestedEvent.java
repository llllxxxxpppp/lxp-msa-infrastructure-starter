package com.lcs.subscription.domain.event;

/**
 * 환불 요청 이벤트.
 *
 * <p>구독권 취소 등에 따라 환불이 필요한 경우 발행되며, {@code PaymentAdapter}가 이 이벤트를
 * 구독하여 PG(stub) 환불을 시도하고 그 결과를 구독권/결제 요청에 반영한다.
 */
public class RefundRequestedEvent extends BaseDomainEvent {

    private final Long subscriptionId;
    private final Long paymentId;

    public RefundRequestedEvent(Long subscriptionId, Long paymentId) {
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
