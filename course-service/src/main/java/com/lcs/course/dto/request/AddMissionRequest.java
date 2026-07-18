package com.lcs.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddMissionRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 4096) String content) {
}
