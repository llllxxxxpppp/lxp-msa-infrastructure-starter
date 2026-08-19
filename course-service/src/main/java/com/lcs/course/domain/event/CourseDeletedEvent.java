package com.lcs.course.domain.event;

public class CourseDeletedEvent extends BaseDomainEvent {

    private final Long courseId;

    public CourseDeletedEvent(Long courseId) {
        super();
        this.courseId = courseId;
    }

    public Long getCourseId() {
        return courseId;
    }
}
