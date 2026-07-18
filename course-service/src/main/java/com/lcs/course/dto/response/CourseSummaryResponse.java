package com.lcs.course.dto.response;

import com.lcs.course.model.entity.Course;

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
