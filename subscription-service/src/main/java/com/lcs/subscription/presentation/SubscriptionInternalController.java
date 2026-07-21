package com.lcs.subscription.presentation;

import com.lcs.subscription.application.dto.request.CreateSubscriptionRequest;
import com.lcs.subscription.application.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/subscriptions")
public class SubscriptionInternalController {

    private final SubscriptionService subscriptionService;

    public SubscriptionInternalController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Void> createFreeSubscription(
            @RequestBody @Valid CreateSubscriptionRequest request) {
        subscriptionService.createFreeSubscriptionIfAbsent(request.memberId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/by-member/{memberId}/suspend")
    public ResponseEntity<Void> suspendSubscriptions(@PathVariable Long memberId) {
        subscriptionService.suspendActiveSubscriptions(memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/by-member/{memberId}/withdraw")
    public ResponseEntity<Void> withdrawMember(@PathVariable Long memberId) {
        subscriptionService.processMemberWithdrawal(memberId);
        return ResponseEntity.noContent().build();
    }
}
