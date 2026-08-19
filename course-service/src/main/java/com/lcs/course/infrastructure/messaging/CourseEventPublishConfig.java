package com.lcs.course.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CourseEventPublishConfig {

    // 커리큘럼 추천봇(소비자)과 합의해야 하는 계약 값
    public static final String EXCHANGE = "course.events";
    public static final String PUBLISHED_ROUTING_KEY = "course.published";
    public static final String UNPUBLISHED_ROUTING_KEY = "course.unpublished";
    public static final String DELETED_ROUTING_KEY = "course.deleted";

    @Bean
    public TopicExchange courseEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
