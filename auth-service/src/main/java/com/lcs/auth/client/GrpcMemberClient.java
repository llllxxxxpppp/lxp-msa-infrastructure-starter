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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.member-client.mode", havingValue = "grpc")
public class GrpcMemberClient implements MemberClient {
    private final MemberLoginInfoServiceGrpc.MemberLoginInfoServiceBlockingStub blockingStub;

    public GrpcMemberClient(Channel memberServiceChannel) {
        this.blockingStub = MemberLoginInfoServiceGrpc.newBlockingStub(memberServiceChannel);
    }

    @Override
    public Optional<MemberLoginInfoResponseDTO> findByEmail(String email) {
        try {
            MemberInfoLoginResponse response = blockingStub.getMemberLoginInfo(
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
