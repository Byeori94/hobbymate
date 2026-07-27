package com.byeori.hobbymate.clubboard.vo;

import java.time.LocalDateTime;

public record ClubNoticeDetail(
        Long postId,
        Long clubId,
        Long writerMemberId,
        String title,
        String content,
        String writerNickname,
        String writerRole,
        String pinnedYn,
        long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public boolean isPinned() {
        return "Y".equals(pinnedYn);
    }

    public String writerDisplayName() {
        return writerNickname == null || writerNickname.isBlank()
                ? "알 수 없는 회원"
                : writerNickname;
    }

    public String writerRoleLabel() {
        return switch (writerRole == null ? "" : writerRole) {
            case "LEADER" -> "모임장";
            case "MANAGER" -> "운영진";
            default -> "";
        };
    }

    public boolean isModified() {
        return createdAt != null && updatedAt != null && updatedAt.isAfter(createdAt);
    }
}
