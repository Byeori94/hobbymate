package com.byeori.hobbymate.member.vo;

public record MemberAuthInfo(
        Long memberId,
        String loginId,
        String password,
        String nickname,
        String profileImageUrl,
        String memberRole,
        String memberStatus) {
}
