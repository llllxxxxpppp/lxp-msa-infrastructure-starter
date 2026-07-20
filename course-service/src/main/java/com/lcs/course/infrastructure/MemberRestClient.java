package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "course.instructor-status.mode", havingValue = "rest")
public class MemberRestClient implements InstructorStatusClient {

    private final RestClient restClient;

    public MemberRestClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("http://member-service").build();
    }

    @Override
    public boolean isSuspended(Long instructorId) {
        SuspensionStatusResponse response = restClient.get()
                .uri("/internal/members/{instructorId}/suspension-status", instructorId)
                .retrieve()
                .body(SuspensionStatusResponse.class);
        return response != null && response.suspended();
    }

    private record SuspensionStatusResponse(boolean suspended) {}
}
