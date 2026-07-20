package com.lcs.member.application.dto.response;

import com.lcs.member.domain.model.entity.Member;

public record SuspensionStatusResponse(boolean suspended) {

    public static SuspensionStatusResponse from(Member member) {
        return new SuspensionStatusResponse(member.isSuspended());
    }
}
