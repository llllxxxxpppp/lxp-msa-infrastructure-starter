package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import com.lcs.grpc.member.MemberSuspensionServiceGrpc;
import com.lcs.grpc.member.SuspensionStatusRequest;
import com.lcs.grpc.member.SuspensionStatusResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Qualifier("instructorStatusClientRaw")
@ConditionalOnProperty(name = "course.instructor-status.mode", havingValue = "grpc")
public class MemberGrpcClient implements InstructorStatusClient {

    private static final long DEADLINE_SECONDS = 3;

    private final MemberSuspensionServiceGrpc.MemberSuspensionServiceBlockingStub stub;

    public MemberGrpcClient(MemberSuspensionServiceGrpc.MemberSuspensionServiceBlockingStub stub) {
        this.stub = stub;
    }

    @Override
    public boolean isSuspended(Long instructorId) {
        // deadline을 걸어 member 무응답 시 스레드가 매달리지 않게 한다.
        // deadline 초과 시 StatusRuntimeException이 나가 CB가 실패로 집계한다.
        SuspensionStatusResponse response = stub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getSuspensionStatus(
                        SuspensionStatusRequest.newBuilder()
                                .setInstructorId(instructorId)
                                .build());
        return response.getSuspended();
    }
}
