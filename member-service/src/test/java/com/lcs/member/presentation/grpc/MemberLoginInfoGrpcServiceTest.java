package com.lcs.member.presentation.grpc;

import com.lcs.grpc.member.MemberInfoLoginRequest;
import com.lcs.grpc.member.MemberInfoLoginResponse;
import com.lcs.grpc.member.MemberLoginInfoServiceGrpc;
import com.lcs.member.application.service.MemberService;
import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.MemberRole;
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
 * MEMBER-21: auth-service의 gRPC 클라이언트(GrpcMemberClient)와 연동될 MemberLoginInfoService 서버 구현 검증.
 * 순수 Mockito 단위 테스트 — MockMvc나 실제 gRPC 서버/채널을 띄우지 않는다(grpc-testing 불필요).
 * 이 gRPC 엔드포인트는 내부 전용이므로 인증/인가(401/403) 시나리오는 검증 대상이 아니다.
 */
@ExtendWith(MockitoExtension.class)
class MemberLoginInfoGrpcServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private StreamObserver<MemberInfoLoginResponse> responseObserver;

    private MemberLoginInfoGrpcService memberLoginInfoGrpcService;

    @BeforeEach
    void setUp() {
        memberLoginInfoGrpcService = new MemberLoginInfoGrpcService(memberService);
    }

    @Test
    @DisplayName("MemberLoginInfoGrpcService는 MemberLoginInfoServiceImplBase를 상속한다")
    void givenMemberLoginInfoGrpcService_whenAssignedToImplBaseType_thenAssignmentSucceeds() {
        MemberLoginInfoServiceGrpc.MemberLoginInfoServiceImplBase implBase = memberLoginInfoGrpcService;

        assertThat(implBase).isNotNull();
        verifyNoInteractions(memberService, responseObserver);
    }

    @Test
    @DisplayName("존재하는 이메일로 조회하면 회원 인증 정보를 담은 응답을 onNext, onCompleted로 전달한다")
    void givenExistingEmail_whenGetMemberLoginInfo_thenRespondsWithCredentialInfo() {
        String email = "member@lcs.com";
        MemberInfoLoginRequest request = MemberInfoLoginRequest.newBuilder()
                .setEmail(email)
                .build();
        com.lcs.member.application.dto.response.MemberCredentialResponse credentialResponse =
                new com.lcs.member.application.dto.response.MemberCredentialResponse(
                        1L, "hashed-password", MemberRole.MEMBER, false, false);
        when(memberService.findByEmailForAuth(email)).thenReturn(credentialResponse);

        memberLoginInfoGrpcService.getMemberLoginInfo(request, responseObserver);

        verify(responseObserver).onNext(argThat(response ->
                response.getMemberId() == 1L
                        && "hashed-password".equals(response.getPassword())
                        && "MEMBER".equals(response.getRole())
                        && !response.getSuspended()
                        && !response.getDeleted()));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
        verify(memberService).findByEmailForAuth(email);
    }

    @Test
    @DisplayName("정지된 강사 이메일로 조회하면 suspended=true, role=INSTRUCTOR 응답을 전달한다")
    void givenSuspendedInstructorEmail_whenGetMemberLoginInfo_thenRespondsWithSuspendedTrueAndInstructorRole() {
        String email = "instructor@lcs.com";
        MemberInfoLoginRequest request = MemberInfoLoginRequest.newBuilder()
                .setEmail(email)
                .build();
        com.lcs.member.application.dto.response.MemberCredentialResponse credentialResponse =
                new com.lcs.member.application.dto.response.MemberCredentialResponse(
                        2L, "hashed-password-2", MemberRole.INSTRUCTOR, true, false);
        when(memberService.findByEmailForAuth(email)).thenReturn(credentialResponse);

        memberLoginInfoGrpcService.getMemberLoginInfo(request, responseObserver);

        verify(responseObserver).onNext(argThat(response ->
                response.getMemberId() == 2L
                        && "hashed-password-2".equals(response.getPassword())
                        && "INSTRUCTOR".equals(response.getRole())
                        && response.getSuspended()
                        && !response.getDeleted()));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
        verify(memberService).findByEmailForAuth(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 NOT_FOUND StatusRuntimeException으로 onError를 호출한다")
    void givenNonExistingEmail_whenGetMemberLoginInfo_thenCallsOnErrorWithNotFound() {
        String email = "unknown@lcs.com";
        MemberInfoLoginRequest request = MemberInfoLoginRequest.newBuilder()
                .setEmail(email)
                .build();
        String errorMessage = "존재하지 않는 회원입니다.";
        when(memberService.findByEmailForAuth(email)).thenThrow(new MemberException(errorMessage));

        memberLoginInfoGrpcService.getMemberLoginInfo(request, responseObserver);

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
        verify(memberService).findByEmailForAuth(email);
    }
}
