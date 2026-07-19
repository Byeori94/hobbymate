package com.byeori.hobbymate.common.validator;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MemberValidationRules {

    public static final int NICKNAME_MAX_LENGTH = 50;
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 255;

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

    public static boolean isValidPassword(String value) {
        return hasValidPasswordLengthAndWhitespace(value)
                && hasRequiredPasswordCategories(value);
    }

    public static boolean hasValidPasswordLengthAndWhitespace(String value) {
        return value != null
                && value.length() >= PASSWORD_MIN_LENGTH
                && value.length() <= PASSWORD_MAX_LENGTH
                && value.chars().noneMatch(Character::isWhitespace);
    }

    public static boolean hasRequiredPasswordCategories(String value) {
        if (value == null) return false;
        int categories = 0;
        if (value.matches(".*[A-Za-z].*")) categories++;
        if (value.matches(".*\\d.*")) categories++;
        if (value.matches(".*[^A-Za-z0-9\\s].*")) categories++;
        return categories >= 2;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
