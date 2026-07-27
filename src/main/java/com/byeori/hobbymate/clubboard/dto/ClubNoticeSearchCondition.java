package com.byeori.hobbymate.clubboard.dto;

public record ClubNoticeSearchCondition(
        Long clubId,
        String searchType,
        String keyword,
        String keywordPattern,
        int page,
        int pageSize) {

    public int offset() {
        return (page - 1) * pageSize;
    }

    public ClubNoticeSearchCondition withPage(int normalizedPage) {
        return new ClubNoticeSearchCondition(
                clubId,
                searchType,
                keyword,
                keywordPattern,
                normalizedPage,
                pageSize);
    }
}
