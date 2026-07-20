package com.lcs.course.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderRequest(
        @NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotNull Type type,
            @NotNull Long id) {
    }

    public enum Type {
        LECTURE,
        MISSION
    }
}
