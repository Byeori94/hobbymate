package com.byeori.hobbymate.common.exception;

public class ClubNoticeUpdateException extends RuntimeException {

    private final String fieldName;

    public ClubNoticeUpdateException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public ClubNoticeUpdateException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
    }

    public String getFieldName() {
        return fieldName;
    }
}
