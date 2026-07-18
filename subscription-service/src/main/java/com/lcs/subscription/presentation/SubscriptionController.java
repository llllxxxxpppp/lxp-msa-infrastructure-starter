package com.lcs.subscription.presentation;

import com.lcs.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.subscription.application.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구독권 조회/취소 API.
 *
 * <p>구독권 생성의 문서화된 트리거는 회원가입 요청(무료)과 시스템 재발급(유료)뿐이므로
 * 수동 생성({@code POST /api/subscriptions})과 재발급({@code POST /api/subscriptions/reissue})
 * 엔드포인트는 제공하지 않는다.
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> get(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionInfo(subscriptionId));
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<Void> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscription(userId, subscriptionId);
        return ResponseEntity.ok().build();
    }
}
