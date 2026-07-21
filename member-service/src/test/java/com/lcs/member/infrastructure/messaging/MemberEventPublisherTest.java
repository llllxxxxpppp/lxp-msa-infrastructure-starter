package com.lcs.member.infrastructure.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lcs.member.domain.event.InstructorSuspendedEvent;
import com.lcs.member.domain.event.MemberRegisteredEvent;
import com.lcs.member.domain.event.MemberSuspendedEvent;
import com.lcs.member.domain.event.MemberWithdrawnEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * MEMBER-22: HTTP 동기 통지({@code HttpMemberStateChangeNotifier})를 대체하는
 * {@link MemberEventPublisher}(RabbitMQ 발행 {@code @TransactionalEventListener})의 핵심 계약을 검증한다.
 *
 * <p>검증 대상:</p>
 * <ul>
 *     <li>각 {@code onXxx} 메서드가 고정 exchange("member.events")와 이벤트별 routingKey로
 *     {@link RabbitTemplate#convertAndSend(String, String, Object)}를 호출하는지(완료 기준 관련 설계)</li>
 *     <li>발행 실패({@link AmqpException}) 시에도 예외가 호출자로 전파되지 않는지(완료 기준 2, "무시+로그" 정책)</li>
 *     <li>MEMBER-12 로깅 계약 — 이벤트 클래스명/eventId/occurredAt이 담긴 INFO 로그가 발행 성공/실패와 무관하게
 *     항상 1건 기록되고, 실패 시에만 ERROR 로그가 추가로 남는지(완료 기준 3)</li>
 * </ul>
 *
 * <p>이 테스트는 Spring 컨텍스트 없이 {@link MemberEventPublisher}를 직접 생성해 각 리스너 메서드를 호출하는
 * 순수 단위 테스트다 — {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 자체의 커밋 이후 발행 동작은
 * Spring 프레임워크가 보장하는 부분이므로 이 테스트의 책임이 아니며, 트랜잭션 커밋 이후 실제 발행 여부는
 * {@code RabbitMqEventPublishingIntegrationTest}에서 별도로 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class MemberEventPublisherTest {

    private static final String EXCHANGE = "member.events";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final Pattern OFFSET_DATE_TIME_PATTERN = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})");

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MemberEventPublisher publisher;

    // -------------------------------------------------------------------------
    // 성공 시나리오: exchange/routingKey/payload 정확히 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("MemberRegisteredEvent가 발생하면 member.events exchange로 member.registered 라우팅 키로 발행된다")
    void givenMemberRegisteredEvent_whenOnMemberRegistered_thenConvertAndSendWithRegisteredRoutingKey() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberRegisteredEvent event = new MemberRegisteredEvent(1L);

        publisher.onMemberRegistered(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.registered"), eq(event));
    }

    @Test
    @DisplayName("MemberSuspendedEvent가 발생하면 member.events exchange로 member.suspended 라우팅 키로 발행된다")
    void givenMemberSuspendedEvent_whenOnMemberSuspended_thenConvertAndSendWithSuspendedRoutingKey() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberSuspendedEvent event = new MemberSuspendedEvent(2L);

        publisher.onMemberSuspended(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.suspended"), eq(event));
    }

    @Test
    @DisplayName("MemberWithdrawnEvent가 발생하면 member.events exchange로 member.withdrawn 라우팅 키로 발행된다")
    void givenMemberWithdrawnEvent_whenOnMemberWithdrawn_thenConvertAndSendWithWithdrawnRoutingKey() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(3L);

        publisher.onMemberWithdrawn(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.withdrawn"), eq(event));
    }

    @Test
    @DisplayName("InstructorSuspendedEvent가 발생하면 member.events exchange로 instructor.suspended 라우팅 키로 발행된다")
    void givenInstructorSuspendedEvent_whenOnInstructorSuspended_thenConvertAndSendWithInstructorSuspendedRoutingKey() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        InstructorSuspendedEvent event = new InstructorSuspendedEvent(4L);

        publisher.onInstructorSuspended(event);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("instructor.suspended"), eq(event));
    }

    // -------------------------------------------------------------------------
    // 실패 시나리오: AmqpException 발생 시 예외를 삼킨다("무시+로그" 정책)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("발행 중 AmqpException이 발생해도 onMemberRegistered는 예외를 전파하지 않는다")
    void givenRabbitTemplateThrowsAmqpException_whenOnMemberRegistered_thenExceptionIsSwallowed() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberRegisteredEvent event = new MemberRegisteredEvent(1L);
        AmqpException brokerDown = new AmqpException("broker unreachable") { };

        willThrow(brokerDown)
                .given(rabbitTemplate)
                .convertAndSend(eq(EXCHANGE), eq("member.registered"), eq(event));

        assertDoesNotThrow(() -> publisher.onMemberRegistered(event));

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.registered"), eq(event));
    }

    @Test
    @DisplayName("발행 중 AmqpException이 발생해도 onInstructorSuspended는 예외를 전파하지 않는다")
    void givenRabbitTemplateThrowsAmqpException_whenOnInstructorSuspended_thenExceptionIsSwallowed() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        InstructorSuspendedEvent event = new InstructorSuspendedEvent(9L);
        AmqpException brokerDown = new AmqpException("broker unreachable") { };

        willThrow(brokerDown)
                .given(rabbitTemplate)
                .convertAndSend(eq(EXCHANGE), eq("instructor.suspended"), eq(event));

        assertDoesNotThrow(() -> publisher.onInstructorSuspended(event));

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("instructor.suspended"), eq(event));
    }

    // -------------------------------------------------------------------------
    // MEMBER-12 로깅 계약: 발행 성공/실패와 무관하게 INFO 로그는 항상 남는다
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("발행에 성공하면 MemberRegisteredEvent의 타입/ID/발생시각이 INFO로 로깅되고 ERROR 로그는 없다")
    void givenSuccessfulPublish_whenOnMemberRegistered_thenLogsInfoOnlyWithEventTypeIdAndOccurredAt() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberRegisteredEvent event = new MemberRegisteredEvent(1L);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> publisher.onMemberRegistered(event));

            assertEventLoggedAtInfo(appender, MemberRegisteredEvent.class.getSimpleName());
            assertEquals(0, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.registered"), eq(event));
    }

    @Test
    @DisplayName("발행에 실패해도 MemberSuspendedEvent는 INFO 이벤트 로그와 ERROR 실패 로그를 모두 남긴다")
    void givenFailedPublish_whenOnMemberSuspended_thenLogsInfoAndErrorTogether() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberSuspendedEvent event = new MemberSuspendedEvent(2L);
        AmqpException brokerDown = new AmqpException("broker unreachable") { };

        willThrow(brokerDown)
                .given(rabbitTemplate)
                .convertAndSend(eq(EXCHANGE), eq("member.suspended"), eq(event));

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> publisher.onMemberSuspended(event));

            assertEventLoggedAtInfo(appender, MemberSuspendedEvent.class.getSimpleName());
            assertEquals(1, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.suspended"), eq(event));
    }

    @Test
    @DisplayName("발행에 성공하면 MemberWithdrawnEvent의 타입/ID/발생시각이 INFO로 로깅되고 ERROR 로그는 없다")
    void givenSuccessfulPublish_whenOnMemberWithdrawn_thenLogsInfoOnlyWithEventTypeIdAndOccurredAt() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        MemberWithdrawnEvent event = new MemberWithdrawnEvent(3L);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> publisher.onMemberWithdrawn(event));

            assertEventLoggedAtInfo(appender, MemberWithdrawnEvent.class.getSimpleName());
            assertEquals(0, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("member.withdrawn"), eq(event));
    }

    @Test
    @DisplayName("발행에 실패해도 InstructorSuspendedEvent는 INFO 이벤트 로그와 ERROR 실패 로그를 모두 남긴다")
    void givenFailedPublish_whenOnInstructorSuspended_thenLogsInfoAndErrorTogether() {
        publisher = new MemberEventPublisher(rabbitTemplate);
        InstructorSuspendedEvent event = new InstructorSuspendedEvent(4L);
        AmqpException brokerDown = new AmqpException("broker unreachable") { };

        willThrow(brokerDown)
                .given(rabbitTemplate)
                .convertAndSend(eq(EXCHANGE), eq("instructor.suspended"), eq(event));

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> publisher.onInstructorSuspended(event));

            assertEventLoggedAtInfo(appender, InstructorSuspendedEvent.class.getSimpleName());
            assertEquals(1, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq("instructor.suspended"), eq(event));
    }

    // -------------------------------------------------------------------------
    // 로그 캡처 헬퍼 (MEMBER-12, HttpMemberStateChangeNotifierTest와 동일 패턴)
    // -------------------------------------------------------------------------

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(MemberEventPublisher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(MemberEventPublisher.class);
        logbackLogger.detachAppender(appender);
        appender.stop();
    }

    private List<ILoggingEvent> logsAtLevel(ListAppender<ILoggingEvent> appender, Level level) {
        return appender.list.stream()
                .filter(loggingEvent -> loggingEvent.getLevel().equals(level))
                .collect(Collectors.toList());
    }

    private void assertEventLoggedAtInfo(ListAppender<ILoggingEvent> appender, String expectedEventType) {
        List<ILoggingEvent> infoLogs = logsAtLevel(appender, Level.INFO);
        assertEquals(1, infoLogs.size());

        String message = infoLogs.get(0).getFormattedMessage();
        assertTrue(message.contains(expectedEventType),
                "Expected message to contain event type [" + expectedEventType + "] but was: " + message);

        String uuidText = extractFirstMatch(UUID_PATTERN, message);
        assertDoesNotThrow(() -> UUID.fromString(uuidText));

        String occurredAtText = extractFirstMatch(OFFSET_DATE_TIME_PATTERN, message);
        assertDoesNotThrow(() -> OffsetDateTime.parse(occurredAtText));
    }

    private String extractFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        assertTrue(matcher.find(), "Expected pattern [" + pattern + "] to be found in: " + text);
        return matcher.group();
    }
}
