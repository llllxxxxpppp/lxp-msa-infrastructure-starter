package com.lcs.auth.client;

import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import com.lcs.auth.exception.MemberServiceUnavailableException;
import com.lcs.grpc.member.MemberInfoLoginRequest;
import com.lcs.grpc.member.MemberInfoLoginResponse;
import com.lcs.grpc.member.MemberLoginInfoServiceGrpc;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Qualifier("memberClientRaw")
@ConditionalOnProperty(name = "auth.member-client.mode", havingValue = "grpc")
public class GrpcMemberClient implements MemberClient {
    private static final long DEADLINE_SECONDS = 3;

    private final MemberLoginInfoServiceGrpc.MemberLoginInfoServiceBlockingStub blockingStub;

    public GrpcMemberClient(Channel memberServiceChannel) {
        this.blockingStub = MemberLoginInfoServiceGrpc.newBlockingStub(memberServiceChannel);
    }

    @Override
    public Optional<MemberLoginInfoResponseDTO> findByEmail(String email) {
        try {
            // deadline을 걸어 member 무응답 시 스레드가 매달리지 않게 한다.
            // deadline 초과 시 StatusRuntimeException이 나가 CB가 실패로 집계한다.
            MemberInfoLoginResponse response = blockingStub
                    .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getMemberLoginInfo(
                            MemberInfoLoginRequest.newBuilder().setEmail(email).build());
            return Optional.of(new MemberLoginInfoResponseDTO(
                    response.getMemberId(),
                    response.getPassword(),
                    response.getRole(),
                    response.getSuspended(),
                    response.getDeleted()));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return Optional.empty();
            }
            throw new MemberServiceUnavailableException(
                    "member-service gRPC 요청에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
