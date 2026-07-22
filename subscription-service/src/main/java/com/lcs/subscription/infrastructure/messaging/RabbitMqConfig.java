package com.lcs.subscription.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String MEMBER_EVENTS_EXCHANGE = "member.events";
    public static final String SUBSCRIPTION_MEMBER_QUEUE = "subscription.member";
    public static final String SUBSCRIPTION_MEMBER_DLX = "subscription.member.dlx";
    public static final String SUBSCRIPTION_MEMBER_DLQ = "subscription.member.dlq";

    @Bean
    public TopicExchange memberEventsExchange() {
        return new TopicExchange(MEMBER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue subscriptionMemberQueue() {
        return QueueBuilder.durable(SUBSCRIPTION_MEMBER_QUEUE)
                .withArgument("x-dead-letter-exchange", SUBSCRIPTION_MEMBER_DLX)
                .withArgument("x-dead-letter-routing-key", SUBSCRIPTION_MEMBER_QUEUE)
                .build();
    }

    @Bean
    public DirectExchange subscriptionMemberDlx() {
        return new DirectExchange(SUBSCRIPTION_MEMBER_DLX, true, false);
    }

    @Bean
    public Queue subscriptionMemberDlq() {
        return QueueBuilder.durable(SUBSCRIPTION_MEMBER_DLQ).build();
    }

    @Bean
    public Binding subscriptionMemberDlqBinding(Queue subscriptionMemberDlq, DirectExchange subscriptionMemberDlx) {
        return BindingBuilder
                .bind(subscriptionMemberDlq)
                .to(subscriptionMemberDlx)
                .with(SUBSCRIPTION_MEMBER_QUEUE);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Binding memberRegisteredBinding(
            Queue subscriptionMemberQueue,
            TopicExchange memberEventsExchange
    ) {
        return BindingBuilder
                .bind(subscriptionMemberQueue)
                .to(memberEventsExchange)
                .with("member.registered");
    }

    @Bean
    public Binding memberSuspendedBinding(
            Queue subscriptionMemberQueue,
            TopicExchange memberEventsExchange
    ) {
        return BindingBuilder
                .bind(subscriptionMemberQueue)
                .to(memberEventsExchange)
                .with("member.suspended");
    }

    @Bean
    public Binding memberWithdrawnBinding(
            Queue subscriptionMemberQueue,
            TopicExchange memberEventsExchange
    ) {
        return BindingBuilder
                .bind(subscriptionMemberQueue)
                .to(memberEventsExchange)
                .with("member.withdrawn");
    }
}
