package com.byeori.hobbymate.member.vo;

import java.time.LocalDate;

public record MemberRegistration(
        String loginId,
        String encodedPassword,
        String name,
        String nickname,
        String email,
        String phone,
        LocalDate birthDate,
        String gender) {
}
