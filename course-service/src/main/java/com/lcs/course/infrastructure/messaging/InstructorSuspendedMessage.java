package com.lcs.course.infrastructure.messaging;

/**
 * member가 강사 정지 시 발행하는 메시지. (routing key: instructor.suspended)
 * member 발행 JSON의 필드명과 일치해야 역직렬화된다: { "instructorId": &lt;Long&gt; }
 */
public record InstructorSuspendedMessage(Long instructorId) {
}
