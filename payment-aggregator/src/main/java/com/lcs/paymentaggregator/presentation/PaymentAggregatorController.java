package com.lcs.paymentaggregator.presentation;

import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/api/payment-aggregate")
public class PaymentAggregatorController {

    private final WebClient webClient;

    public PaymentAggregatorController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/{subscriptionId}")
    public Mono<PaymentAggregateResponse> find(@PathVariable Long subscriptionId) {
        Mono<SubscriptionResponse> subscriptionMono = webClient.get()
                .uri("http://subscription-service/api/subscriptions/{id}", subscriptionId)
                .retrieve()
                .bodyToMono(SubscriptionResponse.class);

        Mono<PaymentResponse> paymentMono = webClient.get()
                .uri("http://payment-service/internal/payments/subscriptions/{id}", subscriptionId)
                .retrieve()
                .bodyToMono(PaymentResponse.class);

        // 두 서비스를 비동기·병렬로 호출하고, 둘 다 완료되면 하나의 응답으로 합칩니다.
        return Mono.zip(subscriptionMono, paymentMono)
                .map(result -> new PaymentAggregateResponse(result.getT1(), result.getT2()));
    }

    public record PaymentAggregateResponse(
            SubscriptionResponse subscription,
            PaymentResponse payment
    ) {
    }

    public record SubscriptionResponse(
            Long subscriptionId,
            Long memberId,
            String plan,
            String status
    ) {
    }

    public record PaymentResponse(
            Long paymentId,
            Long subscriptionId,
            Long amount,
            String status
    ) {
    }
}
