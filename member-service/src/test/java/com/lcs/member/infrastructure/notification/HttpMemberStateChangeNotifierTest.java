package com.lcs.member.infrastructure.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.member.domain.event.InstructorSuspendedEvent;
import com.lcs.member.domain.event.MemberRegisteredEvent;
import com.lcs.member.domain.event.MemberSuspendedEvent;
import com.lcs.member.domain.event.MemberWithdrawnEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MEMBER-09: HttpMemberStateChangeNotifier의 핵심 계약("무시+로그" 정책)을 검증한다.
 *
 * <p>이 테스트는 Mockito mock이 아니라 JDK 내장 {@link HttpServer}로 띄운 실제 목 서버를 상대로
 * {@link HttpMemberStateChangeNotifier}(실제 구현체)를 호출하는 통합 성격의 단위 테스트다.
 * 따라서 검증은 {@code verify()}가 아니라 캡처한 요청/응답에 대한 JUnit 단언(assert*)으로 수행한다.</p>
 *
 * <p>가정: {@code HttpMemberStateChangeNotifier}는
 * {@code new HttpMemberStateChangeNotifier(subscriptionServiceBaseUrl, courseServiceBaseUrl)} 형태의
 * 2-인자 생성자를 가진다(Subscription 대상 호출 3종 + Course 대상 호출 1종을 구분하기 위함).
 * 구현 에이전트가 다른 생성자 형태를 택한다면 이 테스트 파일의 생성자 호출부만 맞춰 조정이 필요하다.</p>
 */
class HttpMemberStateChangeNotifierTest {

    private static final String UNREACHABLE_URL_PLACEHOLDER = "http://127.0.0.1:1";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final Pattern OFFSET_DATE_TIME_PATTERN = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})");

    private final List<HttpServer> startedServers = new ArrayList<>();

    @AfterEach
    void tearDown() {
        startedServers.forEach(server -> server.stop(0));
        startedServers.clear();
    }

    // -------------------------------------------------------------------------
    // 성공 시나리오: 올바른 HTTP 메서드/경로/바디로 요청이 전송되는지 검증 (완료 기준 6)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 서비스가 정상 응답하면 회원가입 통지는 POST /internal/subscriptions 로 memberId 바디를 담아 전송된다")
    void givenRespondingSubscriptionServer_whenNotifyMemberRegistered_thenSendsPostRequestWithMemberIdBody() throws IOException {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer subscriptionServer =
                startCapturingServer(200, capturedMethod, capturedPath, capturedBody, new AtomicInteger());

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        assertDoesNotThrow(() -> notifier.notifyMemberRegistered(123L));

        assertEquals("POST", capturedMethod.get());
        assertEquals("/internal/subscriptions", capturedPath.get());

        Map<?, ?> parsedBody = new ObjectMapper().readValue(capturedBody.get(), Map.class);
        assertEquals(123, ((Number) parsedBody.get("memberId")).intValue());
    }

    @Test
    @DisplayName("구독 서비스가 정상 응답하면 회원정지 통지는 POST /internal/subscriptions/by-member/{id}/suspend 로 전송된다")
    void givenRespondingSubscriptionServer_whenNotifyMemberSuspended_thenSendsPostRequestToSuspendPath() throws IOException {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer subscriptionServer =
                startCapturingServer(200, capturedMethod, capturedPath, capturedBody, new AtomicInteger());

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        assertDoesNotThrow(() -> notifier.notifyMemberSuspended(45L));

        assertEquals("POST", capturedMethod.get());
        assertEquals("/internal/subscriptions/by-member/45/suspend", capturedPath.get());
    }

    @Test
    @DisplayName("구독 서비스가 정상 응답하면 회원탈퇴 통지는 POST /internal/subscriptions/by-member/{id}/withdraw 로 전송된다")
    void givenRespondingSubscriptionServer_whenNotifyMemberWithdrawn_thenSendsPostRequestToWithdrawPath() throws IOException {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer subscriptionServer =
                startCapturingServer(200, capturedMethod, capturedPath, capturedBody, new AtomicInteger());

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        assertDoesNotThrow(() -> notifier.notifyMemberWithdrawn(77L));

        assertEquals("POST", capturedMethod.get());
        assertEquals("/internal/subscriptions/by-member/77/withdraw", capturedPath.get());
    }

    @Test
    @DisplayName("강좌 서비스가 정상 응답하면 강사정지 통지는 POST /internal/courses/by-instructor/{id}/unpublish-all 로 전송된다")
    void givenRespondingCourseServer_whenNotifyInstructorSuspended_thenSendsPostRequestToUnpublishAllPath() throws IOException {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer courseServer =
                startCapturingServer(200, capturedMethod, capturedPath, capturedBody, new AtomicInteger());

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(UNREACHABLE_URL_PLACEHOLDER, baseUrl(courseServer));

        assertDoesNotThrow(() -> notifier.notifyInstructorSuspended(9L));

        assertEquals("POST", capturedMethod.get());
        assertEquals("/internal/courses/by-instructor/9/unpublish-all", capturedPath.get());
    }

    // -------------------------------------------------------------------------
    // 실패 시나리오: 대상 서버 500 또는 연결 불가 상황에서도 예외를 던지지 않고,
    // 재시도 없이 1회만 호출한다 (완료 기준 5, 7)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 서비스가 500을 반환해도 notifyMemberRegistered는 예외 없이 반환하고 재시도하지 않는다")
    void givenSubscriptionServerReturns500_whenNotifyMemberRegistered_thenReturnsNormallyWithoutRetry() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer subscriptionServer = startFailingServer(500, requestCount);

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        assertDoesNotThrow(() -> notifier.notifyMemberRegistered(1L));

        assertEquals(1, requestCount.get());
    }

    @Test
    @DisplayName("구독 서비스에 연결할 수 없어도 notifyMemberSuspended는 예외 없이 반환한다")
    void givenUnreachableSubscriptionServer_whenNotifyMemberSuspended_thenReturnsNormally() throws IOException {
        String unreachableUrl = "http://127.0.0.1:" + closedPort();

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(unreachableUrl, UNREACHABLE_URL_PLACEHOLDER);

        assertDoesNotThrow(() -> notifier.notifyMemberSuspended(2L));
    }

    @Test
    @DisplayName("구독 서비스가 500을 반환해도 notifyMemberWithdrawn은 예외 없이 반환하고 재시도하지 않는다")
    void givenSubscriptionServerReturns500_whenNotifyMemberWithdrawn_thenReturnsNormallyWithoutRetry() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        HttpServer subscriptionServer = startFailingServer(500, requestCount);

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        assertDoesNotThrow(() -> notifier.notifyMemberWithdrawn(3L));

        assertEquals(1, requestCount.get());
    }

    @Test
    @DisplayName("강좌 서비스에 연결할 수 없어도 notifyInstructorSuspended는 예외 없이 반환한다")
    void givenUnreachableCourseServer_whenNotifyInstructorSuspended_thenReturnsNormally() throws IOException {
        String unreachableUrl = "http://127.0.0.1:" + closedPort();

        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(UNREACHABLE_URL_PLACEHOLDER, unreachableUrl);

        assertDoesNotThrow(() -> notifier.notifyInstructorSuspended(4L));
    }

    // -------------------------------------------------------------------------
    // MEMBER-12: INFO 레벨 이벤트 로깅 검증 (완료 기준 1, 3)
    // HTTP 호출 성공 시에도 INFO 로그가 남고, ERROR 로그는 남지 않는다.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 서비스가 정상 응답해도 회원가입 통지는 MemberRegisteredEvent의 타입/ID/발생시각을 INFO로 로깅한다")
    void givenRespondingSubscriptionServer_whenNotifyMemberRegistered_thenLogsInfoWithEventTypeIdAndOccurredAt() throws IOException {
        HttpServer subscriptionServer = startCapturingServer(
                200, new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyMemberRegistered(123L));

            assertEventLoggedAtInfo(appender, MemberRegisteredEvent.class.getSimpleName());
            assertEquals(0, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("구독 서비스가 정상 응답해도 회원정지 통지는 MemberSuspendedEvent의 타입/ID/발생시각을 INFO로 로깅한다")
    void givenRespondingSubscriptionServer_whenNotifyMemberSuspended_thenLogsInfoWithEventTypeIdAndOccurredAt() throws IOException {
        HttpServer subscriptionServer = startCapturingServer(
                200, new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyMemberSuspended(45L));

            assertEventLoggedAtInfo(appender, MemberSuspendedEvent.class.getSimpleName());
            assertEquals(0, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("구독 서비스가 정상 응답해도 회원탈퇴 통지는 MemberWithdrawnEvent의 타입/ID/발생시각을 INFO로 로깅한다")
    void givenRespondingSubscriptionServer_whenNotifyMemberWithdrawn_thenLogsInfoWithEventTypeIdAndOccurredAt() throws IOException {
        HttpServer subscriptionServer = startCapturingServer(
                200, new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyMemberWithdrawn(77L));

            assertEventLoggedAtInfo(appender, MemberWithdrawnEvent.class.getSimpleName());
            assertEquals(0, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("강좌 서비스가 정상 응답해도 강사정지 통지는 InstructorSuspendedEvent의 타입/ID/발생시각을 INFO로 로깅한다")
    void givenRespondingCourseServer_whenNotifyInstructorSuspended_thenLogsInfoWithEventTypeIdAndOccurredAt() throws IOException {
        HttpServer courseServer = startCapturingServer(
                200, new AtomicReference<>(), new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(UNREACHABLE_URL_PLACEHOLDER, baseUrl(courseServer));

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyInstructorSuspended(9L));

            assertEventLoggedAtInfo(appender, InstructorSuspendedEvent.class.getSimpleName());
            assertEquals(0, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    // -------------------------------------------------------------------------
    // MEMBER-12: 실패 시에도 INFO 로그가 남고, 기존 ERROR 로그와 함께 존재한다 (완료 기준 2, 4)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("구독 서비스가 500을 반환해도 회원가입 통지는 INFO 이벤트 로그와 ERROR 실패 로그를 모두 남긴다")
    void givenSubscriptionServerReturns500_whenNotifyMemberRegistered_thenLogsInfoAndErrorTogether() throws IOException {
        HttpServer subscriptionServer = startFailingServer(500, new AtomicInteger());
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyMemberRegistered(1L));

            assertEventLoggedAtInfo(appender, MemberRegisteredEvent.class.getSimpleName());
            assertEquals(1, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("구독 서비스에 연결할 수 없어도 회원정지 통지는 INFO 이벤트 로그와 ERROR 실패 로그를 모두 남긴다")
    void givenUnreachableSubscriptionServer_whenNotifyMemberSuspended_thenLogsInfoAndErrorTogether() throws IOException {
        String unreachableUrl = "http://127.0.0.1:" + closedPort();
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(unreachableUrl, UNREACHABLE_URL_PLACEHOLDER);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyMemberSuspended(2L));

            assertEventLoggedAtInfo(appender, MemberSuspendedEvent.class.getSimpleName());
            assertEquals(1, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("구독 서비스가 500을 반환해도 회원탈퇴 통지는 INFO 이벤트 로그와 ERROR 실패 로그를 모두 남긴다")
    void givenSubscriptionServerReturns500_whenNotifyMemberWithdrawn_thenLogsInfoAndErrorTogether() throws IOException {
        HttpServer subscriptionServer = startFailingServer(500, new AtomicInteger());
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(baseUrl(subscriptionServer), UNREACHABLE_URL_PLACEHOLDER);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyMemberWithdrawn(3L));

            assertEventLoggedAtInfo(appender, MemberWithdrawnEvent.class.getSimpleName());
            assertEquals(1, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    @DisplayName("강좌 서비스에 연결할 수 없어도 강사정지 통지는 INFO 이벤트 로그와 ERROR 실패 로그를 모두 남긴다")
    void givenUnreachableCourseServer_whenNotifyInstructorSuspended_thenLogsInfoAndErrorTogether() throws IOException {
        String unreachableUrl = "http://127.0.0.1:" + closedPort();
        HttpMemberStateChangeNotifier notifier =
                new HttpMemberStateChangeNotifier(UNREACHABLE_URL_PLACEHOLDER, unreachableUrl);

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            assertDoesNotThrow(() -> notifier.notifyInstructorSuspended(4L));

            assertEventLoggedAtInfo(appender, InstructorSuspendedEvent.class.getSimpleName());
            assertEquals(1, logsAtLevel(appender, Level.ERROR).size());
        } finally {
            detachAppender(appender);
        }
    }

    // -------------------------------------------------------------------------
    // 로그 캡처 헬퍼 (MEMBER-12)
    // -------------------------------------------------------------------------

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(HttpMemberStateChangeNotifier.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(HttpMemberStateChangeNotifier.class);
        logbackLogger.detachAppender(appender);
        appender.stop();
    }

    private List<ILoggingEvent> logsAtLevel(ListAppender<ILoggingEvent> appender, Level level) {
        return appender.list.stream()
                .filter(event -> event.getLevel().equals(level))
                .collect(Collectors.toList());
    }

    /**
     * INFO 레벨 로그가 정확히 1건 존재하며, 그 로그(메시지+인자 포함)에
     * 이벤트 타입(클래스 simple name)/UUID로 파싱 가능한 이벤트 ID/OffsetDateTime으로 파싱 가능한
     * 발생 시각이 모두 포함되어 있는지 검증한다.
     */
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

    // -------------------------------------------------------------------------
    // 목 서버 헬퍼
    // -------------------------------------------------------------------------

    private HttpServer startCapturingServer(
            int statusCode,
            AtomicReference<String> capturedMethod,
            AtomicReference<String> capturedPath,
            AtomicReference<String> capturedBody,
            AtomicInteger requestCount
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            capturedMethod.set(exchange.getRequestMethod());
            capturedPath.set(exchange.getRequestURI().getPath());
            byte[] requestBodyBytes = exchange.getRequestBody().readAllBytes();
            capturedBody.set(new String(requestBodyBytes, StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });
        server.start();
        startedServers.add(server);
        return server;
    }

    private HttpServer startFailingServer(int statusCode, AtomicInteger requestCount) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });
        server.start();
        startedServers.add(server);
        return server;
    }

    private String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
