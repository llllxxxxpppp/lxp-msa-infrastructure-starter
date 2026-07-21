package com.lcs.member.infrastructure.messaging;

import com.lcs.member.domain.event.BaseDomainEvent;
import com.lcs.member.domain.event.InstructorSuspendedEvent;
import com.lcs.member.domain.event.MemberRegisteredEvent;
import com.lcs.member.domain.event.MemberSuspendedEvent;
import com.lcs.member.domain.event.MemberWithdrawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@code HttpMemberStateChangeNotifier}(동기 HTTP 통지)를 대체하는 RabbitMQ 발행자.
 *
 * <p>회원가입/정지/탈퇴/강사정지 각 도메인 이벤트를 {@link TransactionalEventListener}로 수신해
 * 트랜잭션 커밋 이후({@link TransactionPhase#AFTER_COMMIT})에만 {@code member.events} 토픽
 * 익스체인지로 발행한다. 발행 실패(브로커 다운 등) 시에도 예외를 밖으로 던지지 않고 ERROR 로그만
 * 남긴다("무시+로그" 정책). 재시도는 수행하지 않는다.</p>
 *
 * <p>MEMBER-12: 발행 성공/실패와 무관하게 이벤트 클래스명/eventId/occurredAt을 INFO 레벨로 항상
 * 로깅한다({@code HttpMemberStateChangeNotifier}의 로깅 계약을 그대로 유지).</p>
 */
@Component
public class MemberEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MemberEventPublisher.class);

    private static final String EXCHANGE = "member.events";

    private final RabbitTemplate rabbitTemplate;

    public MemberEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRegistered(MemberRegisteredEvent event) {
        publish(event, "member.registered");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberSuspended(MemberSuspendedEvent event) {
        publish(event, "member.suspended");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberWithdrawn(MemberWithdrawnEvent event) {
        publish(event, "member.withdrawn");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInstructorSuspended(InstructorSuspendedEvent event) {
        publish(event, "instructor.suspended");
    }

    private void publish(BaseDomainEvent event, String routingKey) {
        logDomainEvent(event);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            if (log.isErrorEnabled()) {
                log.error(
                        "{} publish failed. eventId={}, occurredAt={}",
                        event.getClass().getSimpleName(), event.getEventId(), event.getOccurredAt(), ex);
            }
        }
    }

    private void logDomainEvent(BaseDomainEvent event) {
        if (log.isInfoEnabled()) {
            log.info(
                    "{} occurred. eventId={}, occurredAt={}",
                    event.getClass().getSimpleName(), event.getEventId(), event.getOccurredAt());
        }
    }
}
