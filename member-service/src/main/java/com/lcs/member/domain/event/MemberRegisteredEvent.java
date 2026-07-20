package com.lcs.member.domain.event;

public class MemberRegisteredEvent extends BaseDomainEvent {

    private final Long memberId;

    public MemberRegisteredEvent(Long memberId) {
        super();
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }
}
