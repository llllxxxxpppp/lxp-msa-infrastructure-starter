package com.lcs.member.presentation.grpc;

import com.lcs.grpc.member.MemberSuspensionServiceGrpc;
import com.lcs.grpc.member.SuspensionStatusRequest;
import com.lcs.grpc.member.SuspensionStatusResponse;
import com.lcs.member.application.service.MemberService;
import com.lcs.member.domain.exception.MemberException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class MemberSuspensionGrpcService extends MemberSuspensionServiceGrpc.MemberSuspensionServiceImplBase {

    private final MemberService memberService;

    public MemberSuspensionGrpcService(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public void getSuspensionStatus(SuspensionStatusRequest request,
                                     StreamObserver<SuspensionStatusResponse> responseObserver) {
        try {
            com.lcs.member.application.dto.response.SuspensionStatusResponse result =
                    memberService.getSuspensionStatus(request.getInstructorId());
            SuspensionStatusResponse response = SuspensionStatusResponse.newBuilder()
                    .setSuspended(result.suspended())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (MemberException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
