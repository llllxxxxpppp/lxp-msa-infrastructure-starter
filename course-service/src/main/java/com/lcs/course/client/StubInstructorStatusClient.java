package com.lcs.course.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "course.instructor-status.rest-enabled", havingValue = "false", matchIfMissing = true)
public class StubInstructorStatusClient implements InstructorStatusClient {

    @Override
    public boolean isSuspended(Long instructorId) {
        return false;
    }
}
