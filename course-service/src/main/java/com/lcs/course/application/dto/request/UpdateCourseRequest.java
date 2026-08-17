package com.lcs.course.application.dto.request;

import com.lcs.course.domain.model.vo.Category;
import com.lcs.course.domain.model.vo.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 4096) String description,
        String thumbnailUrl,
        @NotNull Category category,
        @NotNull Difficulty difficulty,
        @Positive int durationMinutes) {
}
