package com.lcs.course.domain.model.vo;

import com.lcs.course.domain.exception.CourseException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Title {

    static final int MAX_LENGTH = 100;

    @Column(name = "title", nullable = false, length = MAX_LENGTH)
    private String value;

    protected Title() {}

    public Title(String value) {
        if (value == null || value.isBlank()) {
            throw new CourseException("제목은 비어있을 수 없습니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new CourseException("제목은 100자를 초과할 수 없습니다.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
