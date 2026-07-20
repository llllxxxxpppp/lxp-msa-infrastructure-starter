package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "course.instructor-status.rest-enabled", havingValue = "true")
public class MemberRestClient implements InstructorStatusClient {

    private final RestClient restClient;

    public MemberRestClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("http://member-service").build();
    }

    @Override
    public boolean isSuspended(Long instructorId) {
        // TODO(member): member-service 강사 정지 조회 API 계약 확정 후 경로/응답 형식 조정
        Boolean suspended = restClient.get()
                .uri("/api/members/{id}/suspended", instructorId)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(suspended);
    }
}
