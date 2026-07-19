package com.lcs.member.domain.notification;

/**
 * 회원 상태 변경(가입/정지/탈퇴, 강사 정지) 사실을 Course/Subscription 등 대상 서비스에 통지하는 포트.
 *
 * <p>구현체는 통지 실패(대상 서비스 연결 불가, 5xx 등) 시에도 예외를 밖으로 던지지 않고
 * 로그만 남긴 뒤 정상 반환해야 한다("무시+로그" 정책). 재시도는 수행하지 않는다.</p>
 */
public interface MemberStateChangeNotifier {

    void notifyMemberRegistered(Long memberId);

    void notifyMemberSuspended(Long memberId);

    void notifyMemberWithdrawn(Long memberId);

    void notifyInstructorSuspended(Long instructorId);
}
