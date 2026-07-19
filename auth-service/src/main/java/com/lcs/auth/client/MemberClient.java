package com.lcs.auth.client;

import com.lcs.auth.client.dto.request.MemberLoginInfoRequestDTO;
import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import com.lcs.auth.exception.MemberServiceUnavailableException;
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

    public Optional<MemberLoginInfoResponseDTO> findByEmail(String email) {
        try {
            return Optional.ofNullable(restClient.post()
                    .uri("http://member-service/members/email")
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
