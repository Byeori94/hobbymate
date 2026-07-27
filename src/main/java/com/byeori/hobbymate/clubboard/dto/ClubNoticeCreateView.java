package com.byeori.hobbymate.clubboard.dto;

public record ClubNoticeCreateView(
        Long clubId,
        String clubName,
        boolean canManageClub,
        boolean canWriteNotice) {
}
