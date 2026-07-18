package com.lcs.member.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
