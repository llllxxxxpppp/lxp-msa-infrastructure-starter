package com.lcs.course.domain.event;

public class CourseUnpublishedEvent extends BaseDomainEvent {

    private final Long courseId;

    public CourseUnpublishedEvent(Long courseId) {
        super();
        this.courseId = courseId;
    }

    public Long getCourseId() {
        return courseId;
    }
}
