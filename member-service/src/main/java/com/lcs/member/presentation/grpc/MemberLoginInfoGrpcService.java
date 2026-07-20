package com.lcs.member.presentation.grpc;

import com.lcs.grpc.member.MemberInfoLoginRequest;
import com.lcs.grpc.member.MemberInfoLoginResponse;
import com.lcs.grpc.member.MemberLoginInfoServiceGrpc;
import com.lcs.member.application.dto.response.MemberCredentialResponse;
import com.lcs.member.application.service.MemberService;
import com.lcs.member.domain.exception.MemberException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class MemberLoginInfoGrpcService extends MemberLoginInfoServiceGrpc.MemberLoginInfoServiceImplBase {

    private final MemberService memberService;

    public MemberLoginInfoGrpcService(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public void getMemberLoginInfo(MemberInfoLoginRequest request,
                                    StreamObserver<MemberInfoLoginResponse> responseObserver) {
        try {
            MemberCredentialResponse result = memberService.findByEmailForAuth(request.getEmail());
            MemberInfoLoginResponse response = MemberInfoLoginResponse.newBuilder()
                    .setMemberId(result.memberId())
                    .setPassword(result.passwordHash())
                    .setRole(result.role().name())
                    .setSuspended(result.suspended())
                    .setDeleted(result.deleted())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (MemberException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
