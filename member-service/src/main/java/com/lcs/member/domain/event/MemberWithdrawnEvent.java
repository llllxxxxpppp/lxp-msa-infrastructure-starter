package com.lcs.member.domain.event;

public class MemberWithdrawnEvent extends BaseDomainEvent {

    private final Long memberId;

    public MemberWithdrawnEvent(Long memberId) {
        super();
        this.memberId = memberId;
    }

    public Long getMemberId() {
        return memberId;
    }
}
