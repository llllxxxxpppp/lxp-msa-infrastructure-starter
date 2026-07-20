package com.lcs.member.application.dto.response;

import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.entity.Member;
import com.lcs.member.domain.model.entity.RegularMember;

public record UserResponseDTO(Long id, String email, MemberRole role) {
    private static final int VISIBLE_EMAIL_PREFIX_LENGTH = 3;

    public static UserResponseDTO from(Member member) {
        String email = member.getEmail();
        if (member instanceof RegularMember regularMember && regularMember.getWithdrawnAt() != null) {
            email = maskEmail(email);
        }
        return new UserResponseDTO(member.getId().value(), email, member.getRole());
    }

    private static String maskEmail(String email) {
        if (email.length() <= VISIBLE_EMAIL_PREFIX_LENGTH) {
            return email;
        }

        StringBuilder maskedEmail = new StringBuilder(email.substring(0, VISIBLE_EMAIL_PREFIX_LENGTH));
        for (int i = VISIBLE_EMAIL_PREFIX_LENGTH; i < email.length(); i++) {
            maskedEmail.append('*');
        }
        return maskedEmail.toString();
    }
}
