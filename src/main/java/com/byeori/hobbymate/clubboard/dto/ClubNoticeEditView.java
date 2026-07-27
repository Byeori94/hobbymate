package com.byeori.hobbymate.clubboard.dto;

public record ClubNoticeEditView(
        Long clubId,
        Long postId,
        String clubName,
        boolean canManageClub,
        String title,
        String content,
        String pinnedYn,
        ClubNoticeReturnQuery returnQuery) {
}
