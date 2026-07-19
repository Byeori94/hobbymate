package com.byeori.hobbymate.member.vo;

public record MemberAuthInfo(
        Long memberId,
        String loginId,
        String password,
        String nickname,
        String memberRole,
        String memberStatus) {
}
