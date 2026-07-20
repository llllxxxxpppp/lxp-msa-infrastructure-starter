package com.lcs.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import com.lcs.auth.exception.MemberServiceUnavailableException;
import com.lcs.grpc.member.MemberInfoLoginRequest;
import com.lcs.grpc.member.MemberInfoLoginResponse;
import com.lcs.grpc.member.MemberLoginInfoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GrpcMemberClient 단위 테스트")
class GrpcMemberClientTest {

    private Server server;
    private ManagedChannel channel;

    private MemberClient startServer(MemberLoginInfoServiceGrpc.MemberLoginInfoServiceImplBase serviceImpl)
            throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(serviceImpl)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        return new GrpcMemberClient(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    @DisplayName("member-service가 정상 응답하면 회원 정보를 담은 Optional을 반환한다")
    void findByEmail_memberExists_returnsDto() throws IOException {
        MemberClient memberClient = startServer(new MemberLoginInfoServiceGrpc.MemberLoginInfoServiceImplBase() {
            @Override
            public void getMemberLoginInfo(MemberInfoLoginRequest request,
                    StreamObserver<MemberInfoLoginResponse> responseObserver) {
                assertThat(request.getEmail()).isEqualTo("user@test.com");
                responseObserver.onNext(MemberInfoLoginResponse.newBuilder()
                        .setId(1L)
                        .setPassword("encoded")
                        .setRole("USER")
                        .setSuspended(false)
                        .setDeleted(false)
                        .build());
                responseObserver.onCompleted();
            }
        });

        Optional<MemberLoginInfoResponseDTO> result = memberClient.findByEmail("user@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().role()).isEqualTo("USER");
        assertThat(result.get().suspended()).isFalse();
        assertThat(result.get().deleted()).isFalse();
    }

    @Test
    @DisplayName("member-service가 NOT_FOUND로 응답하면 빈 Optional을 반환한다")
    void findByEmail_memberNotFound_returnsEmptyOptional() throws IOException {
        MemberClient memberClient = startServer(new MemberLoginInfoServiceGrpc.MemberLoginInfoServiceImplBase() {
            @Override
            public void getMemberLoginInfo(MemberInfoLoginRequest request,
                    StreamObserver<MemberInfoLoginResponse> responseObserver) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            }
        });

        Optional<MemberLoginInfoResponseDTO> result = memberClient.findByEmail("missing@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("member-service가 오류를 반환하면 MemberServiceUnavailableException을 던진다")
    void findByEmail_memberServiceError_throwsMemberServiceUnavailableException() throws IOException {
        MemberClient memberClient = startServer(new MemberLoginInfoServiceGrpc.MemberLoginInfoServiceImplBase() {
            @Override
            public void getMemberLoginInfo(MemberInfoLoginRequest request,
                    StreamObserver<MemberInfoLoginResponse> responseObserver) {
                responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
            }
        });

        assertThatThrownBy(() -> memberClient.findByEmail("user@test.com"))
                .isInstanceOf(MemberServiceUnavailableException.class);
    }
}
