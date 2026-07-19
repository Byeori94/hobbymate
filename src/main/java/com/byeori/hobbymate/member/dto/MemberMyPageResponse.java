package com.byeori.hobbymate.member.dto;

public record MemberMyPageResponse(
        String loginId,
        String nickname,
        String name,
        String email,
        String maskedPhone,
        String gender,
        String birthDate,
        String joinedDate,
        String profileImageUrl) {
}
