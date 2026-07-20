package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Course;

public record CourseSummaryResponse(
        Long courseId,
        Long instructorId,
        String title,
        String status,
        String thumbnailUrl) {

    public static CourseSummaryResponse from(Course course) {
        return new CourseSummaryResponse(
                course.getId().value(),
                course.getInstructorId().value(),
                course.getTitle().getValue(),
                course.getStatus().name(),
                course.getThumbnailUrl());
    }
}
