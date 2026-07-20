package com.lcs.auth.client;

import com.lcs.auth.client.dto.request.MemberLoginInfoRequestDTO;
import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import com.lcs.auth.exception.MemberServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class MemberClient {
    private final RestClient restClient;

    public MemberClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    // fail-fast: fallback을 두지 않는다. 회로 OPEN 시 CallNotPermittedException이 즉시 던져져
    // 호출 스레드가 대기 없이 실패하고, GlobalExceptionHandler가 이를 503으로 매핑한다.
    // 회로 CLOSED 중 5xx/timeout은 MemberServiceUnavailableException으로 나가 실패로 집계된다.
    @CircuitBreaker(name = "member-service")
    public Optional<MemberLoginInfoResponseDTO> findByEmail(String email) {
        try {
            return Optional.ofNullable(restClient.post()
                    .uri("http://member-service/internal/members/by-email/info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MemberLoginInfoRequestDTO(email))
                    .retrieve()
                    .body(MemberLoginInfoResponseDTO.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new MemberServiceUnavailableException(
                    "member-service 요청에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
