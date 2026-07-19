package com.lcs.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("MemberClient 단위 테스트")
class MemberClientTest {

    private MockRestServiceServer mockServer;
    private MemberClient memberClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        memberClient = new MemberClient(builder);
    }

    @Test
    @DisplayName("member-service가 200으로 응답하면 회원 정보를 담은 Optional을 반환한다")
    void findByEmail_memberExists_returnsDto() {
        mockServer.expect(requestTo("http://member-service/members/email"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"email":"user@test.com"}
                        """))
                .andRespond(withSuccess(
                        """
                        {"id":1,"email":"user@test.com","password":"encoded","deleted":false,"role":"USER"}
                        """,
                        MediaType.APPLICATION_JSON));

        Optional<MemberLoginInfoResponseDTO> result = memberClient.findByEmail("user@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("user@test.com");
        assertThat(result.get().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("member-service가 404로 응답하면 빈 Optional을 반환한다")
    void findByEmail_memberNotFound_returnsEmptyOptional() {
        mockServer.expect(requestTo("http://member-service/members/email"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"email":"missing@test.com"}
                        """))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<MemberLoginInfoResponseDTO> result = memberClient.findByEmail("missing@test.com");

        assertThat(result).isEmpty();
    }
}
