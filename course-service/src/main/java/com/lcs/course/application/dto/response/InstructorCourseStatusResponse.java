package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Course;

public record InstructorCourseStatusResponse(Long courseId, Long instructorId, String title, String status) {

    public static InstructorCourseStatusResponse from(Course course) {
        return new InstructorCourseStatusResponse(
                course.getId().value(),
                course.getInstructorId().value(),
                course.getTitle().getValue(),
                course.getStatus().name());
    }
}
