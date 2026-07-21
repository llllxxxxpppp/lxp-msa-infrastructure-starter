package com.lcs.member.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MEMBER-22: 회원 상태 변경 이벤트를 RabbitMQ로 발행하기 위한 설정.
 *
 * <p>{@code member.events"} 토픽 익스체인지 하나만 선언한다. 라우팅 키(예: {@code member.registered},
 * {@code member.suspended}, {@code member.withdrawn}, {@code instructor.suspended})는
 * {@link MemberEventPublisher}가 이벤트별로 고정 문자열을 사용한다.</p>
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "member.events";

    @Bean
    public TopicExchange memberEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return rabbitTemplate;
    }
}
