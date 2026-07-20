package com.lcs.member.application.dto.response;

import com.lcs.member.domain.model.entity.Member;

public record CreateMemberResponse(Long memberId) {

    public static CreateMemberResponse from(Member member) {
        return new CreateMemberResponse(member.getId().value());
    }
}
