package com.lcs.subscription.domain.model.vo;

/**
 * 결제 요청의 종류를 표현한다. PAYMENT(결제 요청) / REFUND(환불 요청).
 */
public enum RequestType {
    PAYMENT,
    REFUND
}
