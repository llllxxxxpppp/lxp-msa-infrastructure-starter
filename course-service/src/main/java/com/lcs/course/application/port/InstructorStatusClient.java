package com.lcs.course.application.port;

public interface InstructorStatusClient {

    boolean isSuspended(Long instructorId);
}
