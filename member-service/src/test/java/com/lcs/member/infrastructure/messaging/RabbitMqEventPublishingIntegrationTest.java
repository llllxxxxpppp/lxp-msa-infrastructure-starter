package com.lcs.member.infrastructure.messaging;

import com.lcs.member.application.dto.response.UserResponseDTO;
import com.lcs.member.application.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MEMBER-22: {@code MemberService}가 실제로 RabbitMQ 브로커까지 이벤트를 발행하는지 확인하는
 * end-to-end 통합 테스트.
 *
 * <p><b>사전 준비</b>: 이 테스트를 실행하려면 로컬에서 실제 RabbitMQ 브로커가 떠 있어야 한다.
 * 저장소 루트의 {@code docker compose up -d rabbitmq}(또는 전체 {@code docker compose up -d})를
 * 먼저 실행할 것. Testcontainers는 사용하지 않으며, {@code src/main/resources/application.yml}의
 * {@code spring.rabbitmq.*} 설정으로 연결한다.</p>
 *
 * <p>이 클래스는 {@code rabbitmq-integration} 태그가 붙어 있어 기본 {@code ./gradlew check}(test 태스크)에서
 * 제외되도록 설정될 예정이다(build.gradle 쪽은 code-implementation-agent가 처리). 브로커가 없는 환경에서는
 * 이 테스트를 개별 실행하면 컨텍스트 로딩 또는 연결 단계에서 실패한다 — 이는 의도된 동작이다.</p>
 */
@Tag("rabbitmq-integration")
@SpringBootTest
class RabbitMqEventPublishingIntegrationTest {

    private static final long RECEIVE_TIMEOUT_MILLIS = 10_000L;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private TopicExchange memberEventsExchange;

    private AnonymousQueue probeQueue;

    @AfterEach
    void tearDown() {
        if (probeQueue != null) {
            amqpAdmin.deleteQueue(probeQueue.getName());
        }
    }

    @Test
    @DisplayName("회원가입 트랜잭션이 커밋되면 member.events exchange로 발행된 메시지가 임시 큐에서 실제로 수신된다")
    void givenAnonymousQueueBoundToAllRoutingKeys_whenRegister_thenMessageIsReceivedFromRealBroker() {
        probeQueue = new AnonymousQueue();
        amqpAdmin.declareQueue(probeQueue);
        Binding binding = BindingBuilder.bind(probeQueue).to(memberEventsExchange).with("#");
        amqpAdmin.declareBinding(binding);

        String email = "rabbitmq-integration-" + UUID.randomUUID() + "@example.com";
        UserResponseDTO registered = memberService.register(email, "password123");

        Message message = rabbitTemplate.receive(probeQueue.getName(), RECEIVE_TIMEOUT_MILLIS);

        assertNotNull(message, "Expected a message to be published to member.events for member registration, "
                + "but none was received within " + RECEIVE_TIMEOUT_MILLIS + "ms. "
                + "Is a real RabbitMQ broker running (docker compose up -d rabbitmq)?");

        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        assertTrue(body.contains(String.valueOf(registered.id())),
                "Expected published message body to contain the registered memberId but was: " + body);
    }
}
