package com.lcs.course.domain.exception;

import java.io.Serial;

public class CourseAccessDeniedException extends CourseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CourseAccessDeniedException(String message) {
        super(message);
    }
}
