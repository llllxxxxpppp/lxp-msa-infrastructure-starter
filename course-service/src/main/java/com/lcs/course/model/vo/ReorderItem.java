package com.lcs.course.model.vo;

import java.util.Objects;

public record ReorderItem(SortableType type, Long id) {

    public ReorderItem {
        Objects.requireNonNull(type, "type은 null일 수 없습니다.");
        Objects.requireNonNull(id, "id는 null일 수 없습니다.");
    }
}
