package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Qualifier("instructorStatusClientRaw")
@ConditionalOnProperty(name = "course.instructor-status.mode", havingValue = "stub", matchIfMissing = true)
public class StubInstructorStatusClient implements InstructorStatusClient {

    @Override
    public boolean isSuspended(Long instructorId) {
        return false;
    }
}
