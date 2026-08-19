package com.lcs.course.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcs.course.domain.event.CourseDeletedEvent;
import com.lcs.course.domain.event.CoursePublishedEvent;
import com.lcs.course.domain.event.CourseUnpublishedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseEventPublisherTest {

    private static final Long COURSE_ID = 30017L;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("공개 이벤트를 course.published 라우팅 키로 발행한다")
    void onCoursePublished_sendsWithPublishedRoutingKey() {
        CourseEventPublisher publisher = new CourseEventPublisher(rabbitTemplate);
        CoursePublishedEvent event = new CoursePublishedEvent(COURSE_ID);

        publisher.onCoursePublished(event);

        verify(rabbitTemplate).convertAndSend(eq("course.events"), eq("course.published"), eq(event));
    }

    @Test
    @DisplayName("비공개 이벤트를 course.unpublished 라우팅 키로 발행한다")
    void onCourseUnpublished_sendsWithUnpublishedRoutingKey() {
        CourseEventPublisher publisher = new CourseEventPublisher(rabbitTemplate);
        CourseUnpublishedEvent event = new CourseUnpublishedEvent(COURSE_ID);

        publisher.onCourseUnpublished(event);

        verify(rabbitTemplate).convertAndSend(eq("course.events"), eq("course.unpublished"), eq(event));
    }

    @Test
    @DisplayName("삭제 이벤트를 course.deleted 라우팅 키로 발행한다")
    void onCourseDeleted_sendsWithDeletedRoutingKey() {
        CourseEventPublisher publisher = new CourseEventPublisher(rabbitTemplate);
        CourseDeletedEvent event = new CourseDeletedEvent(COURSE_ID);

        publisher.onCourseDeleted(event);

        verify(rabbitTemplate).convertAndSend(eq("course.events"), eq("course.deleted"), eq(event));
    }

    @Test
    @DisplayName("발행이 실패해도 예외를 호출자에게 전파하지 않는다")
    void publish_whenBrokerFails_doesNotPropagate() {
        CourseEventPublisher publisher = new CourseEventPublisher(rabbitTemplate);
        willThrow(new AmqpException("broker down"))
                .given(rabbitTemplate).convertAndSend(eq("course.events"), eq("course.published"), (Object) any());

        assertDoesNotThrow(() -> publisher.onCoursePublished(new CoursePublishedEvent(COURSE_ID)));
    }

    @Test
    @DisplayName("발행되는 JSON은 eventId·occurredAt·courseId 세 필드만 담고 occurredAt은 ISO 문자열이다")
    void event_serializesToBotContract() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
                    MessageConverter converter =
                            new RabbitConfig().jsonMessageConverter(context.getBean(ObjectMapper.class));
                    Message message =
                            converter.toMessage(new CoursePublishedEvent(COURSE_ID), new MessageProperties());

                    JsonNode json = new ObjectMapper()
                            .readTree(new String(message.getBody(), StandardCharsets.UTF_8));

                    assertEquals(3, json.size());
                    assertEquals(COURSE_ID, json.get("courseId").asLong());
                    assertNotNull(UUID.fromString(json.get("eventId").asText()));
                    assertTrue(json.get("occurredAt").isTextual());
                    assertNotNull(OffsetDateTime.parse(json.get("occurredAt").asText()));
                });
    }
}
