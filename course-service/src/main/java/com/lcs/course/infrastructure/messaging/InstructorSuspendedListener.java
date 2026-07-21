package com.lcs.course.infrastructure.messaging;

import com.lcs.course.application.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 강사 정지 이벤트를 구독해 해당 강사의 공개 강좌를 비공개 처리한다.
 * (REST의 {@code POST /internal/courses/by-instructor/{id}/unpublish-all}을 비동기 이벤트로 대체)
 */
@Component
public class InstructorSuspendedListener {

    private static final Logger log = LoggerFactory.getLogger(InstructorSuspendedListener.class);

    private final CourseService courseService;

    public InstructorSuspendedListener(CourseService courseService) {
        this.courseService = courseService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handle(InstructorSuspendedMessage message) {
        log.info("InstructorSuspended 이벤트 수신: instructorId={}", message.instructorId());
        courseService.unpublishAllByInstructor(message.instructorId());
    }
}
