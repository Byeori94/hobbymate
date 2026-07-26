package com.byeori.hobbymate.admin.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminMemberVerificationState(
        Long memberId,
        String loginId,
        String nickname,
        String name,
        LocalDate birthDate,
        String gender,
        String phone,
        String memberRole,
        String memberStatus,
        String identityVerifiedYn,
        String verificationMethod,
        LocalDateTime verifiedAt,
        Long verifiedBy,
        String ciHash) {
}
