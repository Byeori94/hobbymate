package com.byeori.hobbymate.club.dto;

public record ClubSearchCondition(
        String searchType,
        String keyword,
        String keywordPattern,
        Long categoryId,
        String region,
        String regionPattern,
        String genderPolicy,
        Integer age,
        String recruitStatus,
        String sortType,
        int page,
        int pageSize) {

    public int offset() {
        return (page - 1) * pageSize;
    }

    public ClubSearchCondition withPage(int normalizedPage) {
        return new ClubSearchCondition(
                searchType,
                keyword,
                keywordPattern,
                categoryId,
                region,
                regionPattern,
                genderPolicy,
                age,
                recruitStatus,
                sortType,
                normalizedPage,
                pageSize);
    }
}
