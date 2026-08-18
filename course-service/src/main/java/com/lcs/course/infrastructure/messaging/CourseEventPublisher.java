package com.lcs.course.infrastructure.messaging;

import com.lcs.course.domain.event.BaseDomainEvent;
import com.lcs.course.domain.event.CourseDeletedEvent;
import com.lcs.course.domain.event.CoursePublishedEvent;
import com.lcs.course.domain.event.CourseUnpublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CourseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CourseEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public CourseEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCoursePublished(CoursePublishedEvent event) {
        publish(event, CourseEventPublishConfig.PUBLISHED_ROUTING_KEY);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseUnpublished(CourseUnpublishedEvent event) {
        publish(event, CourseEventPublishConfig.UNPUBLISHED_ROUTING_KEY);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseDeleted(CourseDeletedEvent event) {
        publish(event, CourseEventPublishConfig.DELETED_ROUTING_KEY);
    }

    private void publish(BaseDomainEvent event, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(CourseEventPublishConfig.EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            if (log.isErrorEnabled()) {
                log.error(
                        "{} publish failed. eventId={}, occurredAt={}, routingKey={}",
                        event.getClass().getSimpleName(), event.getEventId(), event.getOccurredAt(), routingKey, ex);
            }
        }
    }
}
