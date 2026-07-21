package com.lcs.course.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * course가 member 이벤트를 구독하기 위한 RabbitMQ 설정.
 *
 * <p>member(발행)와 동일한 exchange/routing key를 사용해야 메시지가 전달된다.
 * course는 {@code instructor.suspended} 키의 메시지를 받아 강좌를 비공개 처리한다.
 */
@Configuration
public class RabbitConfig {

    // member(발행)와 합의해야 하는 계약 값
    public static final String EXCHANGE = "member.events";
    public static final String QUEUE = "course.instructor-suspended";
    public static final String ROUTING_KEY = "instructor.suspended";

    @Bean
    public TopicExchange memberExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue instructorSuspendedQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding instructorSuspendedBinding(Queue instructorSuspendedQueue, TopicExchange memberExchange) {
        return BindingBuilder.bind(instructorSuspendedQueue).to(memberExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
