package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Qualifier("instructorStatusClientRaw")
@ConditionalOnProperty(name = "course.instructor-status.mode", havingValue = "rest")
public class MemberRestClient implements InstructorStatusClient {

    private final RestClient restClient;

    public MemberRestClient(RestClient.Builder restClientBuilder) {
        // member 무응답 시 스레드가 매달리지 않도록 timeout을 둔다. 끊긴 호출은 CB가 실패로 집계한다.
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        this.restClient = restClientBuilder.baseUrl("http://member-service")
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
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
