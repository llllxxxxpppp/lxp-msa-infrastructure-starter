package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import com.lcs.grpc.member.MemberSuspensionServiceGrpc;
import com.lcs.grpc.member.SuspensionStatusRequest;
import com.lcs.grpc.member.SuspensionStatusResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "course.instructor-status.mode", havingValue = "grpc")
public class MemberGrpcClient implements InstructorStatusClient {

    private final MemberSuspensionServiceGrpc.MemberSuspensionServiceBlockingStub stub;

    public MemberGrpcClient(MemberSuspensionServiceGrpc.MemberSuspensionServiceBlockingStub stub) {
        this.stub = stub;
    }

    @Override
    public boolean isSuspended(Long instructorId) {
        SuspensionStatusResponse response = stub.getSuspensionStatus(
                SuspensionStatusRequest.newBuilder()
                        .setInstructorId(instructorId)
                        .build());
        return response.getSuspended();
    }
}
