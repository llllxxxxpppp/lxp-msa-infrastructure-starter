package com.lcs.course.domain.event;

public class CoursePublishedEvent extends BaseDomainEvent {

    private final Long courseId;

    public CoursePublishedEvent(Long courseId) {
        super();
        this.courseId = courseId;
    }

    public Long getCourseId() {
        return courseId;
    }
}
