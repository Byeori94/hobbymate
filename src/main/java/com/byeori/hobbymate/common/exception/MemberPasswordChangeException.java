package com.byeori.hobbymate.common.exception;

public class MemberPasswordChangeException extends RuntimeException {

    private final String field;

    public MemberPasswordChangeException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
