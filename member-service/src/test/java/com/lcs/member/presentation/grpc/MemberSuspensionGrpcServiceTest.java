package com.lcs.member.presentation.grpc;

import com.lcs.grpc.member.MemberSuspensionServiceGrpc;
import com.lcs.grpc.member.SuspensionStatusRequest;
import com.lcs.grpc.member.SuspensionStatusResponse;
import com.lcs.member.application.service.MemberService;
import com.lcs.member.domain.exception.MemberException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MEMBER-20: course-service의 gRPC 클라이언트와 연동될 MemberSuspensionService 서버 구현 검증.
 * 순수 Mockito 단위 테스트 — MockMvc나 실제 gRPC 서버/채널을 띄우지 않는다(grpc-testing 불필요).
 * 이 gRPC 엔드포인트는 내부 전용이므로 인증/인가(401/403) 시나리오는 검증 대상이 아니다.
 */
@ExtendWith(MockitoExtension.class)
class MemberSuspensionGrpcServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private StreamObserver<SuspensionStatusResponse> responseObserver;

    private MemberSuspensionGrpcService memberSuspensionGrpcService;

    @BeforeEach
    void setUp() {
        memberSuspensionGrpcService = new MemberSuspensionGrpcService(memberService);
    }

    @Test
    @DisplayName("MemberSuspensionGrpcService는 MemberSuspensionServiceImplBase를 상속한다")
    void givenMemberSuspensionGrpcService_whenAssignedToImplBaseType_thenAssignmentSucceeds() {
        MemberSuspensionServiceGrpc.MemberSuspensionServiceImplBase implBase = memberSuspensionGrpcService;

        assertThat(implBase).isNotNull();
        verifyNoInteractions(memberService, responseObserver);
    }

    @Test
    @DisplayName("존재하는 강사 ID로 정지 상태를 조회하면 suspended=true 응답을 onNext, onCompleted로 전달한다")
    void givenExistingInstructorId_whenGetSuspensionStatus_thenRespondsWithSuspendedTrue() {
        long instructorId = 1L;
        SuspensionStatusRequest request = SuspensionStatusRequest.newBuilder()
                .setInstructorId(instructorId)
                .build();
        when(memberService.getSuspensionStatus(instructorId))
                .thenReturn(new com.lcs.member.application.dto.response.SuspensionStatusResponse(true));

        memberSuspensionGrpcService.getSuspensionStatus(request, responseObserver);

        verify(responseObserver).onNext(argThat(SuspensionStatusResponse::getSuspended));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
        verify(memberService).getSuspensionStatus(instructorId);
    }

    @Test
    @DisplayName("존재하는 비강사 회원 ID로 조회해도 suspended=false 응답을 onNext, onCompleted로 전달한다")
    void givenExistingNonInstructorMemberId_whenGetSuspensionStatus_thenRespondsWithSuspendedFalse() {
        long memberId = 2L;
        SuspensionStatusRequest request = SuspensionStatusRequest.newBuilder()
                .setInstructorId(memberId)
                .build();
        when(memberService.getSuspensionStatus(memberId))
                .thenReturn(new com.lcs.member.application.dto.response.SuspensionStatusResponse(false));

        memberSuspensionGrpcService.getSuspensionStatus(request, responseObserver);

        verify(responseObserver).onNext(argThat(response -> !response.getSuspended()));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
        verify(memberService).getSuspensionStatus(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 NOT_FOUND StatusRuntimeException으로 onError를 호출한다")
    void givenNonExistingId_whenGetSuspensionStatus_thenCallsOnErrorWithNotFound() {
        long instructorId = 999L;
        SuspensionStatusRequest request = SuspensionStatusRequest.newBuilder()
                .setInstructorId(instructorId)
                .build();
        String errorMessage = "존재하지 않는 회원입니다.";
        when(memberService.getSuspensionStatus(instructorId))
                .thenThrow(new MemberException(errorMessage));

        memberSuspensionGrpcService.getSuspensionStatus(request, responseObserver);

        verify(responseObserver).onError(argThat(throwable -> {
            if (!(throwable instanceof StatusRuntimeException)) {
                return false;
            }
            StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
            return statusRuntimeException.getStatus().getCode() == Status.Code.NOT_FOUND
                    && errorMessage.equals(statusRuntimeException.getStatus().getDescription());
        }));
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(memberService).getSuspensionStatus(instructorId);
    }
}
