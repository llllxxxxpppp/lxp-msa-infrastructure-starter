package com.lcs.course.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddLectureRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String contentUrl,
        @NotBlank String contentType) {
}
