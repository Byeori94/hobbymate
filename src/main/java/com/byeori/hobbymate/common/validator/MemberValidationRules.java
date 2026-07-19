package com.byeori.hobbymate.common.validator;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MemberValidationRules {

    public static final int NICKNAME_MAX_LENGTH = 50;
    public static final int EMAIL_MAX_LENGTH = 255;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    private MemberValidationRules() {
    }

    public static String normalizeNickname(String value) {
        return trim(value);
    }

    public static String normalizeEmail(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    public static boolean isValidNickname(String value) {
        return value != null && !value.isBlank() && value.length() <= NICKNAME_MAX_LENGTH;
    }

    public static boolean isValidEmail(String value) {
        return value != null
                && value.length() <= EMAIL_MAX_LENGTH
                && EMAIL_PATTERN.matcher(value).matches();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
