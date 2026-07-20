package com.lcs.member.domain.event;

public class InstructorSuspendedEvent extends BaseDomainEvent {

    private final Long instructorId;

    public InstructorSuspendedEvent(Long instructorId) {
        super();
        this.instructorId = instructorId;
    }

    public Long getInstructorId() {
        return instructorId;
    }
}
