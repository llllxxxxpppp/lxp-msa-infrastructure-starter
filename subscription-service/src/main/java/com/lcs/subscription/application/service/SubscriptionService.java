package com.lcs.subscription.application.service;

import com.lcs.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.subscription.domain.event.PaymentRequestedEvent;
import com.lcs.subscription.domain.event.RefundRequestedEvent;
import com.lcs.subscription.domain.exception.SubscriptionException;
import com.lcs.subscription.domain.model.entity.Payment;
import com.lcs.subscription.domain.model.entity.Subscription;
import com.lcs.subscription.domain.model.vo.RequestType;
import com.lcs.subscription.domain.model.vo.ResponseResult;
import com.lcs.subscription.domain.repository.SubscriptionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 구독권 생성/취소/조회 유스케이스.
 *
 * <p>결제/환불은 더 이상 직접 호출하지 않고 이벤트({@code PaymentRequestedEvent},
 * {@code RefundRequestedEvent})를 발행하는 방식으로 전환되었다. 실제 PG 호출과 결과 반영은
 * {@code PaymentAdapter}의 이벤트 리스너 책임이다.
 */
@Service
@Transactional
public class SubscriptionService {

    private static final long FREE_PRICE = 0L;
    private static final long PAID_PRICE = 19_800L;
    private static final long SOLE_PAID_SUBSCRIPTION_COUNT = 1L;

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
    }

    public SubscriptionResponse createSubscription(Long memberId, Long price) {
        Subscription subscription = Subscription.create(memberId, price);
        Payment payment = Payment.create(RequestType.PAYMENT);
        subscription.addPayment(payment);

        if (price == FREE_PRICE) {
            payment.markRequested();
            payment.markResponded(ResponseResult.SUCCESS);
            subscription.activate();
            subscription = subscriptionRepository.save(subscription);
        } else {
            subscription = subscriptionRepository.save(subscription);
            Payment savedPayment = subscription.getPayments().get(0);
            eventPublisher.publishEvent(
                    new PaymentRequestedEvent(subscription.getId(), savedPayment.getId().value()));
        }

        return SubscriptionResponse.from(subscription);
    }

    /**
     * 본인 소유의 구독권만 취소할 수 있다. 환불 정책({@link #isEligibleForRefund})을 만족하면
     * 환불 요청을 추가하고 저장하여 ID를 확보한 뒤 {@code RefundRequestedEvent}를 발행하며, 이
     * 경우 {@code subscription.cancel()}은 호출하지 않는다(정지 처리는 이후 환불 성공 응답을
     * 수신하는 이벤트 리스너의 책임이다). 환불 정책을 만족하지 않으면 이벤트 발행 없이
     * {@code subscription.cancel()}만 호출한다.
     */
    public void cancelSubscription(Long memberId, Long subscriptionId) {
        Subscription subscription = findSubscription(subscriptionId);

        if (!subscription.getMemberId().equals(memberId)) {
            throw new SubscriptionException("본인의 구독권만 취소할 수 있습니다.");
        }

        if (isEligibleForRefund(subscription)) {
            requestRefund(subscription);
        } else {
            subscription.cancel();
            subscriptionRepository.save(subscription);
        }
    }

    /**
     * 대상 회원의 구독권 중 정지 대상(활성화됨 && 정지 안 됨 && 취소 안 됨)인 것 전부를
     * 정지시킨 뒤 저장한다. 이미 정지/취소되었거나 아직 활성화되지 않은 구독권은 건드리지
     * 않는다.
     */
    public void suspendActiveSubscriptions(Long memberId) {
        List<Subscription> subscriptions = subscriptionRepository.findByMemberId(memberId);
        for (Subscription subscription : subscriptions) {
            if (isEligibleForSuspension(subscription)) {
                subscription.suspend();
                subscriptionRepository.save(subscription);
            }
        }
    }

    /**
     * 회원 탈퇴 처리를 수행한다. 대상 회원의 활성 구독권 목록 중 환불 정책
     * ({@link #isEligibleForRefund})을 만족하는 구독권이 있으면(정책상 최대 1개만 가능하다),
     * 그 구독권은 직접 정지하지 않고 환불 요청만 수행한다(정지는 이후 환불 성공 응답을 수신하는
     * 이벤트 리스너의 책임이다). 나머지 활성 구독권은 각각 정지시켜 저장한다. 환불 정책을
     * 만족하는 구독권이 없으면 활성 구독권 전부를 정지시켜 저장한다.
     */
    public void processMemberWithdrawal(Long memberId) {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByMemberId(memberId).stream()
                .filter(this::isEligibleForSuspension)
                .toList();

        for (Subscription subscription : activeSubscriptions) {
            if (isEligibleForRefund(subscription)) {
                requestRefund(subscription);
            } else {
                subscription.suspend();
                subscriptionRepository.save(subscription);
            }
        }
    }

    /**
     * 매일 0시 배치가 호출하는 만료 임박 구독권 자동 재발급 유스케이스.
     *
     * <p>활성화됨 && 정지 안 됨 && 취소 안 됨 상태인 구독권 후보군을 조회한 뒤, 그중
     * {@link Subscription#isEligibleForReissue()}가 true인 것만 유료({@value #PAID_PRICE}원)로
     * 재발급한다. 재발급된 새 구독권 기준으로 결제 요청을 추가/저장한 뒤
     * {@code PaymentRequestedEvent}를 발행한다. 원본 구독권은 변경하거나 저장하지 않는다.
     */
    public void reissueExpiringSubscriptions() {
        List<Subscription> candidates =
                subscriptionRepository.findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull();

        for (Subscription candidate : candidates) {
            if (candidate.isEligibleForReissue()) {
                reissue(candidate);
            }
        }
    }

    private void reissue(Subscription candidate) {
        Subscription reissued = candidate.reissue(PAID_PRICE);
        Payment payment = Payment.create(RequestType.PAYMENT);
        reissued.addPayment(payment);

        reissued = subscriptionRepository.save(reissued);
        Payment savedPayment = reissued.getPayments().get(0);
        eventPublisher.publishEvent(
                new PaymentRequestedEvent(reissued.getId(), savedPayment.getId().value()));
    }

    private void requestRefund(Subscription subscription) {
        Payment refundPayment = Payment.create(RequestType.REFUND);
        subscription.addPayment(refundPayment);
        subscriptionRepository.save(subscription);
        eventPublisher.publishEvent(
                new RefundRequestedEvent(subscription.getId(), refundPayment.getId().value()));
    }

    private boolean isEligibleForSuspension(Subscription subscription) {
        return subscription.getActivatedAt() != null
                && subscription.getSuspendedAt() == null
                && subscription.getCancelledAt() == null;
    }

    /**
     * 환불 정책을 만족하는지 판정한다. 다음 조건을 모두 만족해야 한다.
     * <ul>
     *     <li>대상 구독권이 유료({@code price > 0})이다.</li>
     *     <li>해당 회원에게 발급된 전체 구독권 중 유료 구독권의 개수가 정확히 1개이다.</li>
     *     <li>대상 구독권이 현재 활성 상태({@link Subscription#isValid()})이다.</li>
     *     <li>대상 구독권이 활성화 후 환불 허용 기간 이내({@link Subscription#isWithinRefundPeriod()})이다.</li>
     * </ul>
     */
    private boolean isEligibleForRefund(Subscription subscription) {
        boolean isPaid = subscription.getPrice() > FREE_PRICE;
        if (!isPaid) {
            return false;
        }

        List<Subscription> memberSubscriptions = subscriptionRepository.findByMemberId(subscription.getMemberId());
        long paidCount = memberSubscriptions.stream()
                .filter(memberSubscription -> memberSubscription.getPrice() > FREE_PRICE)
                .count();
        if (paidCount != SOLE_PAID_SUBSCRIPTION_COUNT) {
            return false;
        }

        return subscription.isValid() && subscription.isWithinRefundPeriod();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionInfo(Long subscriptionId) {
        return SubscriptionResponse.from(findSubscription(subscriptionId));
    }

    private Subscription findSubscription(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException("구독권을 찾을 수 없습니다."));
    }
}
