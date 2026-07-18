package com.lcs.member.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link MemberStateChangeNotifier}의 동기 HTTP 구현체.
 *
 * <p>Subscription/Course 서비스에 각각 별도의 {@link RestClient}로 1회 POST 요청을 보낸다.
 * 대상 서비스가 실패(연결 불가, 5xx 등)해도 예외를 던지지 않고 ERROR 레벨 로그만 남긴다("무시+로그" 정책).
 * 재시도는 수행하지 않는다.</p>
 */
@Component
public class HttpMemberStateChangeNotifier implements MemberStateChangeNotifier {

    private static final Logger log = LoggerFactory.getLogger(HttpMemberStateChangeNotifier.class);

    private final RestClient subscriptionServiceClient;
    private final RestClient courseServiceClient;

    public HttpMemberStateChangeNotifier(
            @Value("${member.notification.subscription-service.base-url}") String subscriptionServiceBaseUrl,
            @Value("${member.notification.course-service.base-url}") String courseServiceBaseUrl) {
        this.subscriptionServiceClient = RestClient.builder()
                .baseUrl(subscriptionServiceBaseUrl)
                .build();
        this.courseServiceClient = RestClient.builder()
                .baseUrl(courseServiceBaseUrl)
                .build();
    }

    @Override
    public void notifyMemberRegistered(Long memberId) {
        notifySubscriptionService("MemberRegistered", memberId, () ->
                subscriptionServiceClient.post()
                        .uri("/internal/subscriptions")
                        .body(Map.of("memberId", memberId))
                        .retrieve()
                        .toBodilessEntity());
    }

    @Override
    public void notifyMemberSuspended(Long memberId) {
        notifySubscriptionService("MemberSuspended", memberId, () ->
                subscriptionServiceClient.post()
                        .uri("/internal/subscriptions/by-member/{memberId}/suspend", memberId)
                        .retrieve()
                        .toBodilessEntity());
    }

    @Override
    public void notifyMemberWithdrawn(Long memberId) {
        notifySubscriptionService("MemberWithdrawn", memberId, () ->
                subscriptionServiceClient.post()
                        .uri("/internal/subscriptions/by-member/{memberId}/withdraw", memberId)
                        .retrieve()
                        .toBodilessEntity());
    }

    @Override
    public void notifyInstructorSuspended(Long instructorId) {
        try {
            courseServiceClient.post()
                    .uri("/internal/courses/by-instructor/{instructorId}/unpublish-all", instructorId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            if (log.isErrorEnabled()) {
                log.error(
                        "InstructorSuspended notification failed. instructorId={}, occurredAt={}",
                        instructorId, OffsetDateTime.now(), ex);
            }
        }
    }

    private void notifySubscriptionService(String eventType, Long memberId, Supplier<?> call) {
        try {
            call.get();
        } catch (RestClientException ex) {
            if (log.isErrorEnabled()) {
                log.error(
                        "{} notification failed. memberId={}, occurredAt={}",
                        eventType, memberId, OffsetDateTime.now(), ex);
            }
        }
    }
}
