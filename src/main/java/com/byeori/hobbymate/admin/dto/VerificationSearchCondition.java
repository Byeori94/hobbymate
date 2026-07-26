package com.byeori.hobbymate.admin.dto;

import java.util.Set;

public record VerificationSearchCondition(String searchType, String keyword) {

    private static final Set<String> TYPES =
            Set.of("ALL", "LOGIN_ID", "NICKNAME", "NAME", "PHONE");

    public VerificationSearchCondition {
        searchType = TYPES.contains(searchType) ? searchType : "ALL";
        keyword = keyword == null ? "" : keyword.trim();
    }

    public static VerificationSearchCondition of(String searchType, String keyword) {
        return new VerificationSearchCondition(searchType, keyword);
    }
}
