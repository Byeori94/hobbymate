package com.byeori.hobbymate.clubboard.dto;

public record ClubNoticeReturnQuery(
        int page,
        int pageSize,
        String searchType,
        String keyword) {
}
