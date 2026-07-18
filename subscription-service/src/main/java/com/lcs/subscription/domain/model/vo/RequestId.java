package com.lcs.subscription.domain.model.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 결제/환불 요청을 식별하는 멱등키(idempotency key)를 표현하는 VO.
 *
 * <p>생성자 자체는 UUID 버전을 검증하지 않지만, {@link #generate()}로 생성하면
 * 항상 UUIDv4 값을 갖는다.
 */
public record RequestId(UUID value) {

    public RequestId {
        Objects.requireNonNull(value, "RequestId 값은 null일 수 없습니다.");
    }

    public static RequestId generate() {
        return new RequestId(UUID.randomUUID());
    }
}
