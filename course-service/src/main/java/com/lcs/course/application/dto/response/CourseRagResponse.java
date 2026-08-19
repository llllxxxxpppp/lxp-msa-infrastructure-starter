package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Course;

public record CourseRagResponse(
        Long courseId,
        Long instructorId,
        String title,
        String description,
        String category,
        String categoryLabel,
        String difficulty,
        String difficultyLabel,
        int durationMinutes) {

    public static CourseRagResponse from(Course course) {
        return new CourseRagResponse(
                course.getId().value(),
                course.getInstructorId().value(),
                course.getTitle().getValue(),
                course.getDescription(),
                course.getCategory().name(),
                course.getCategory().getLabel(),
                course.getDifficulty().name(),
                course.getDifficulty().getLabel(),
                course.getDurationMinutes());
    }
}
