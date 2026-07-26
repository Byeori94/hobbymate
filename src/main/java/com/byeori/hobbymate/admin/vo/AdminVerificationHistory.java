package com.byeori.hobbymate.admin.vo;

public record AdminVerificationHistory(
        Long adminMemberId,
        Long targetMemberId,
        String actionType,
        String reason,
        String beforeVerifiedYn,
        String afterVerifiedYn,
        String beforeMethod,
        String afterMethod,
        String beforeCiHash,
        String afterCiHash,
        String processingType,
        String operationId) {
}
