package com.lcs.course.exception;

import java.io.Serial;

public class CourseAccessDeniedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CourseAccessDeniedException(String message) {
        super(message);
    }
}
