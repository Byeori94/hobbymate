package com.byeori.hobbymate.common.exception;

public class ClubCreationException extends RuntimeException {

    private final String fieldName;
    private final boolean accessDenied;

    public ClubCreationException(String fieldName, String message) {
        this(fieldName, message, false);
    }

    public ClubCreationException(String fieldName, String message, boolean accessDenied) {
        super(message);
        this.fieldName = fieldName;
        this.accessDenied = accessDenied;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isAccessDenied() {
        return accessDenied;
    }
}
