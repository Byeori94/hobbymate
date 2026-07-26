package com.byeori.hobbymate.admin.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminMemberVerificationRow(
        Long memberId,
        String loginId,
        String nickname,
        String name,
        LocalDate birthDate,
        String gender,
        String phone,
        LocalDateTime createdAt,
        String verificationMethod,
        LocalDateTime verifiedAt,
        String verifiedByNickname) {

    public String maskedPhone() {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        return digits.length() == 11
                ? digits.substring(0, 3) + "-****-" + digits.substring(7)
                : "-";
    }

    public String genderLabel() {
        if ("FEMALE".equals(gender)) return "여성";
        if ("MALE".equals(gender)) return "남성";
        return "-";
    }

    public String verificationMethodLabel() {
        return "ADMIN_TEMP".equals(verificationMethod) ? "임시 본인인증" : "-";
    }
}
