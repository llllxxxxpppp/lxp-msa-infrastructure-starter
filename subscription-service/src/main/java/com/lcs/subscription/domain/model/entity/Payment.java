package com.lcs.subscription.domain.model.entity;

import com.lcs.subscription.domain.model.vo.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 결제/환불 요청 애그리거트.
 *
 * <p>멱등키(RequestId)와 요청 타입(RequestType: PAYMENT/REFUND)은 생성 시 설정되는
 * 불변 필드이며, 요청 전송 일시/응답 수신 일시/응답 결과(ResponseResult)는 가변 필드다.
 * 상태 전이 검증과 같은 비즈니스 로직은 인프라스트럭처 레이어의 책임이며 이 엔티티는
 * 데이터를 보관하고 요청/응답 사실을 기록하는 동작만 제공한다.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = RequestIdConverter.class)
    @Column(name = "request_id", nullable = false, updatable = false, unique = true)
    private RequestId requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, updatable = false)
    private RequestType requestType;

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_result", nullable = false)
    private ResponseResult responseResult;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    protected Payment() {}

    /**
     * 요청 타입을 입력받아 결제/환불 요청을 생성한다.
     * 멱등키(RequestId)는 내부에서 UUIDv4로 자동 생성된다.
     */
    public static Payment create(RequestType requestType) {
        Objects.requireNonNull(requestType, "requestType은 null일 수 없습니다.");

        Payment payment = new Payment();
        payment.requestId = RequestId.generate();
        payment.requestType = requestType;
        payment.responseResult = ResponseResult.NOT_REQUESTED;
        payment.createdAt = OffsetDateTime.now();
        return payment;
    }

    public PaymentId getId() {
        return new PaymentId(id);
    }

    public RequestId getRequestId() {
        return requestId;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public ResponseResult getResponseResult() {
        return responseResult;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 외부 결제 시스템으로 요청을 전송한 사실을 기록한다. */
    public void markRequested() {
        requestedAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    /** 외부 결제 시스템으로부터 응답을 수신한 사실을 기록한다. */
    public void markResponded(ResponseResult result) {
        Objects.requireNonNull(result, "응답 결과는 null일 수 없습니다.");

        respondedAt = OffsetDateTime.now();
        responseResult = result;
        updatedAt = OffsetDateTime.now();
    }
}
