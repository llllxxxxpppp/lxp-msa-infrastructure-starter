package com.lcs.course.domain.exception;

// TODO(common): common 모듈 배치 후 extends DomainException 으로 교체
public class CourseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CourseException(String message) {
        super(message);
    }
}
