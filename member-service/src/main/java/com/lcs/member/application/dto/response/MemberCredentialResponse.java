package com.lcs.member.application.dto.response;

import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.entity.Member;

public record MemberCredentialResponse(
        Long memberId,
        String passwordHash,
        MemberRole role,
        boolean suspended,
        boolean deleted
) {

    public static MemberCredentialResponse from(Member member) {
        return new MemberCredentialResponse(
                member.getId().value(),
                member.getPassword(),
                member.getRole(),
                member.isSuspended(),
                member.isDeleted());
    }
}
