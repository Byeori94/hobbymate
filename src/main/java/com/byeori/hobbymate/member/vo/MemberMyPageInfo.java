package com.byeori.hobbymate.member.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberMyPageInfo(
        String loginId,
        String nickname,
        String name,
        String email,
        String phone,
        String gender,
        LocalDate birthDate,
        LocalDateTime createdAt,
        String profileImageUrl) {
}
