package com.lcs.member.infrastructure.notification;

import com.lcs.member.domain.event.BaseDomainEvent;
import com.lcs.member.domain.event.InstructorSuspendedEvent;
import com.lcs.member.domain.event.MemberRegisteredEvent;
import com.lcs.member.domain.event.MemberSuspendedEvent;
import com.lcs.member.domain.event.MemberWithdrawnEvent;
import com.lcs.member.domain.notification.MemberStateChangeNotifier;
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
 *
 * <p>MEMBER-12: Lxp-backend 모놀리스의 {@code DomainEventLogger}(모든 도메인 이벤트 발행을 INFO로 로깅)를
 * 대체하기 위해, 각 통지 메서드는 대응하는 도메인 이벤트 인스턴스를 생성해 이벤트 타입/ID/발생 시각을
 * INFO 레벨로 로깅한다. 이 이벤트 객체는 로그 값 추출 용도로만 사용되며 Spring 이벤트로 발행되지 않는다.
 * 이 INFO 로그는 HTTP 호출 성공/실패와 무관하게 항상 남는다.</p>
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
        logDomainEvent(new MemberRegisteredEvent(memberId));
        notifySubscriptionService("MemberRegistered", memberId, () ->
                subscriptionServiceClient.post()
                        .uri("/internal/subscriptions")
                        .body(Map.of("memberId", memberId))
                        .retrieve()
                        .toBodilessEntity());
    }

    @Override
    public void notifyMemberSuspended(Long memberId) {
        logDomainEvent(new MemberSuspendedEvent(memberId));
        notifySubscriptionService("MemberSuspended", memberId, () ->
                subscriptionServiceClient.post()
                        .uri("/internal/subscriptions/by-member/{memberId}/suspend", memberId)
                        .retrieve()
                        .toBodilessEntity());
    }

    @Override
    public void notifyMemberWithdrawn(Long memberId) {
        logDomainEvent(new MemberWithdrawnEvent(memberId));
        notifySubscriptionService("MemberWithdrawn", memberId, () ->
                subscriptionServiceClient.post()
                        .uri("/internal/subscriptions/by-member/{memberId}/withdraw", memberId)
                        .retrieve()
                        .toBodilessEntity());
    }

    @Override
    public void notifyInstructorSuspended(Long instructorId) {
        logDomainEvent(new InstructorSuspendedEvent(instructorId));
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

    private void logDomainEvent(BaseDomainEvent event) {
        if (log.isInfoEnabled()) {
            log.info(
                    "{} occurred. eventId={}, occurredAt={}",
                    event.getClass().getSimpleName(), event.getEventId(), event.getOccurredAt());
        }
    }
}
