package com.lcs.member.application.dto.response;

import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.entity.Member;

public record MemberAuthStatusResponse(MemberRole role, boolean suspended, boolean deleted) {

    public static MemberAuthStatusResponse from(Member member) {
        return new MemberAuthStatusResponse(member.getRole(), member.isSuspended(), member.isDeleted());
    }
}
