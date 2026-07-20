package com.lcs.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import com.lcs.auth.exception.MemberServiceUnavailableException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("RestMemberClient 단위 테스트")
class RestMemberClientTest {

    private MockRestServiceServer mockServer;
    private MemberClient memberClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        memberClient = new RestMemberClient(builder);
    }

    @Test
    @DisplayName("member-service가 200으로 응답하면 회원 정보를 담은 Optional을 반환한다")
    void findByEmail_memberExists_returnsDto() {
        mockServer.expect(requestTo("http://member-service/internal/members/by-email/info"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"email":"user@test.com"}
                        """))
                .andRespond(withSuccess(
                        """
                        {"memberId":1,"passwordHash":"encoded","role":"USER","suspended":false,"deleted":false}
                        """,
                        MediaType.APPLICATION_JSON));

        Optional<MemberLoginInfoResponseDTO> result = memberClient.findByEmail("user@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().memberId()).isEqualTo(1L);
        assertThat(result.get().role()).isEqualTo("USER");
        assertThat(result.get().suspended()).isFalse();
        assertThat(result.get().deleted()).isFalse();
    }

    @Test
    @DisplayName("member-service가 404로 응답하면 빈 Optional을 반환한다")
    void findByEmail_memberNotFound_returnsEmptyOptional() {
        mockServer.expect(requestTo("http://member-service/internal/members/by-email/info"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"email":"missing@test.com"}
                        """))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<MemberLoginInfoResponseDTO> result = memberClient.findByEmail("missing@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("member-service가 5xx로 응답하면 MemberServiceUnavailableException을 던진다")
    void findByEmail_memberServiceServerError_throwsMemberServiceUnavailableException() {
        mockServer.expect(requestTo("http://member-service/internal/members/by-email/info"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> memberClient.findByEmail("user@test.com"))
                .isInstanceOf(MemberServiceUnavailableException.class);
    }
}
