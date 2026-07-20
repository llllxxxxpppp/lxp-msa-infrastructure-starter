package com.lcs.member.domain.event;

public class MemberSuspendedEvent extends BaseDomainEvent {

    private final Long memberId;

    public MemberSuspendedEvent(Long memberId) {
        super();
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }
}
