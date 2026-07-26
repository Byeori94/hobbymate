package com.byeori.hobbymate.category.dto;

import java.util.Set;

public record CategorySearchCondition(
        String keyword,
        String keywordPattern,
        String useYn) {

    private static final Set<String> USE_FILTERS = Set.of("ALL", "Y", "N");

    public static CategorySearchCondition of(String rawKeyword, String rawUseYn) {
        String keyword = trimToNull(rawKeyword);
        if (keyword != null && keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
        }
        String useYn = rawUseYn == null ? "ALL" : rawUseYn.trim().toUpperCase();
        if (!USE_FILTERS.contains(useYn)) {
            useYn = "ALL";
        }
        return new CategorySearchCondition(
                keyword,
                keyword == null ? null : "%" + escapeLike(keyword) + "%",
                useYn);
    }

    public boolean isDefault() {
        return keyword == null && "ALL".equals(useYn);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
