package com.lcs.subscription.domain.model.entity;

import com.lcs.subscription.domain.exception.SubscriptionException;
import com.lcs.subscription.domain.model.vo.ResponseResult;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 구독권 루트 애그리거트.
 *
 * <p>상태는 단일 enum이 아닌 활성화/정지/취소 각각의 nullable 일시로 표현한다.
 * 재발급 체인은 부모 구독권 ID / 구독 시작 일시 / 구독 회차로 추적하며,
 * 유효기간은 달력 월(Calendar Month) 기준으로 계산한다.
 *
 * <p>Payment는 이 애그리거트에 속한 자식 엔티티이므로, 외부에서는 Payment를 직접
 * 변경하지 않고 이 루트가 제공하는 메서드(markPaymentRequested/markPaymentResponded)를
 * 통해서만 상태를 변경한다.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    /** 부모 구독권 ID가 이 값이면 자신이 구독권 체인의 루트임을 뜻한다. */
    private static final long ROOT_PARENT_ID = 0L;

    /** 최초 생성 시 구독 회차 기본값. */
    private static final long INITIAL_GENERATION = 1L;

    /** 유효기간 만료 이 일수 이내이면 재발급 대상으로 간주한다. */
    private static final int REISSUE_ELIGIBLE_DAYS_BEFORE_EXPIRY = 2;

    /** 활성화 시점으로부터 이 일수 이내에 취소하면 환불 대상으로 간주한다. */
    private static final int REFUND_PERIOD_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private Long price;

    @Column(name = "parent_id", nullable = false, updatable = false)
    private Long parentId;

    @Column(name = "subscription_start_at", nullable = false, updatable = false)
    private OffsetDateTime subscriptionStartAt;

    @Column(nullable = false, updatable = false)
    private Long generation;

    @Column(name = "valid_until", nullable = false, updatable = false)
    private OffsetDateTime validUntil;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "subscription_id", nullable = false)
    private List<Payment> payments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    protected Subscription() {}

    /**
     * 신규 구독권을 생성한다. 부모 구독권 ID는 0(루트)으로, 구독 회차는 1로 설정되며
     * 구독 시작 일시는 생성 시점으로 주입된다.
     */
    public static Subscription create(Long memberId, Long price) {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다.");
        Objects.requireNonNull(price, "price는 null일 수 없습니다.");

        Subscription subscription = new Subscription();
        subscription.memberId = memberId;
        subscription.price = price;
        subscription.parentId = ROOT_PARENT_ID;
        subscription.subscriptionStartAt = OffsetDateTime.now();
        subscription.generation = INITIAL_GENERATION;
        subscription.validUntil = calculateValidUntil(subscription.subscriptionStartAt, subscription.generation);
        subscription.createdAt = OffsetDateTime.now();
        return subscription;
    }

    /**
     * 현재 구독권을 원본으로 하여 재발급한다.
     * 소유 회원 ID와 구독 시작 일시는 원본에서 그대로 복사되고, 구독 회차는 원본+1,
     * 부모 구독권 ID는 원본의 id로 설정된다. 가격은 새로 입력받고 유효기간은 재산정된다.
     */
    public Subscription reissue(Long price) {
        Objects.requireNonNull(price, "price는 null일 수 없습니다.");

        Subscription reissued = new Subscription();
        reissued.memberId = this.memberId;
        reissued.price = price;
        reissued.parentId = this.id;
        reissued.subscriptionStartAt = this.subscriptionStartAt;
        reissued.generation = this.generation + 1;
        reissued.validUntil = calculateValidUntil(reissued.subscriptionStartAt, reissued.generation);
        reissued.createdAt = OffsetDateTime.now();
        return reissued;
    }

    /**
     * 달력 월 기준 유효기간 계산법.
     * (구독 시작 일시).plusMonths(구독 회차).plusDays(1).truncatedTo(DAYS).
     * java.time의 plusMonths는 계산된 날짜가 대상 월에 존재하지 않으면
     * 해당 월의 마지막 날로 자동 클램프한다.
     */
    private static OffsetDateTime calculateValidUntil(OffsetDateTime subscriptionStartAt, Long generation) {
        return subscriptionStartAt
                .plusMonths(generation)
                .plusDays(1)
                .truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * 구독권을 활성화한다. 이미 활성화된 구독권을 다시 활성화하는 것은 허용하지 않는다.
     */
    public void activate() {
        if (isActivated()) {
            throw new SubscriptionException("이미 활성화된 구독권입니다.");
        }
        activatedAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    /**
     * 구독권을 정지한다. 활성화되지 않았거나, 이미 정지되었거나, 이미 취소된 구독권은
     * 정지할 수 없다(정지와 취소는 상호배타 상태이다).
     */
    public void suspend() {
        if (!isActivated()) {
            throw new SubscriptionException("활성화되지 않은 구독권은 정지할 수 없습니다.");
        }
        if (isSuspended()) {
            throw new SubscriptionException("이미 정지된 구독권입니다.");
        }
        if (isCancelled()) {
            throw new SubscriptionException("취소된 구독권은 정지할 수 없습니다.");
        }
        suspendedAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    /**
     * 구독권을 취소한다. 활성화되지 않았거나, 이미 취소되었거나, 이미 정지된 구독권은
     * 취소할 수 없다(정지와 취소는 상호배타 상태이다).
     */
    public void cancel() {
        if (!isActivated()) {
            throw new SubscriptionException("활성화되지 않은 구독권은 취소할 수 없습니다.");
        }
        if (isCancelled()) {
            throw new SubscriptionException("이미 취소된 구독권입니다.");
        }
        if (isSuspended()) {
            throw new SubscriptionException("정지된 구독권은 취소할 수 없습니다.");
        }
        cancelledAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    /** 활성화 여부 true && 정지 여부 false && 유효기간 안 지남 (취소 여부는 무관). */
    public boolean isValid() {
        return isActivated() && !isSuspended() && !isExpired();
    }

    /** 활성화 true && 정지 false && 취소 false && 유효기간 만료 2일 이내(아직 지나지는 않음). */
    public boolean isEligibleForReissue() {
        return isActivated() && !isSuspended() && !isCancelled() && isNearExpiry();
    }

    /**
     * 활성화 일시로부터 환불 허용 기간({@value #REFUND_PERIOD_DAYS}일) 이내인지 여부.
     * 활성화되지 않은(activatedAt이 null인) 구독권은 환불 대상이 아니므로 false를 반환한다.
     */
    public boolean isWithinRefundPeriod() {
        if (activatedAt == null) {
            return false;
        }
        return OffsetDateTime.now().isBefore(activatedAt.plusDays(REFUND_PERIOD_DAYS));
    }

    private boolean isActivated() {
        return activatedAt != null;
    }

    private boolean isSuspended() {
        return suspendedAt != null;
    }

    private boolean isCancelled() {
        return cancelledAt != null;
    }

    private boolean isExpired() {
        return !OffsetDateTime.now().isBefore(validUntil);
    }

    /**
     * 유효기간까지 남은 기간이 재발급 허용 일수 이내이면서, 아직 만료되지 않은 경우에만 true.
     * 하한(아직 지나지 않음)과 상한(임박함)을 모두 검사하여, 이미 오래 전에 만료된
     * 구독권이 재발급 대상으로 잘못 판정되는 것을 방지한다.
     */
    private boolean isNearExpiry() {
        OffsetDateTime now = OffsetDateTime.now();
        return !isExpired() && now.plusDays(REISSUE_ELIGIBLE_DAYS_BEFORE_EXPIRY).isAfter(validUntil);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPrice() {
        return price;
    }

    public Long getParentId() {
        return parentId;
    }

    public OffsetDateTime getSubscriptionStartAt() {
        return subscriptionStartAt;
    }

    public Long getGeneration() {
        return generation;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public OffsetDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 결제/환불 요청 내역을 하나의 리스트로 통합 조회한다. 각 Payment의 RequestType으로 구분한다. */
    public List<Payment> getPayments() {
        return List.copyOf(payments);
    }

    /** 신규 결제/환불 요청을 내역 리스트에 추가한다. */
    public void addPayment(Payment payment) {
        Objects.requireNonNull(payment, "payment는 null일 수 없습니다.");
        payments.add(payment);
    }

    /**
     * 지정한 결제/환불 요청에 대해 외부 결제 시스템으로 요청을 전송한 사실을 기록한다.
     * Payment는 이 애그리거트의 자식 엔티티이므로 외부에서 직접 변경하지 않고 이 메서드를
     * 통해서만 변경한다.
     */
    public void markPaymentRequested(Long paymentId) {
        findPayment(paymentId).markRequested();
    }

    /**
     * 지정한 결제/환불 요청에 대해 외부 결제 시스템으로부터 응답을 수신한 사실을 기록한다.
     * Payment는 이 애그리거트의 자식 엔티티이므로 외부에서 직접 변경하지 않고 이 메서드를
     * 통해서만 변경한다.
     */
    public void markPaymentResponded(Long paymentId, ResponseResult result) {
        findPayment(paymentId).markResponded(result);
    }

    private Payment findPayment(Long paymentId) {
        Objects.requireNonNull(paymentId, "paymentId는 null일 수 없습니다.");
        return payments.stream()
                .filter(payment -> payment.getId().value().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new SubscriptionException("결제 요청을 찾을 수 없습니다."));
    }
}
