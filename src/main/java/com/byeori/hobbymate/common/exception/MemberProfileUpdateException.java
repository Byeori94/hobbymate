package com.byeori.hobbymate.common.exception;

public class MemberProfileUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public MemberProfileUpdateException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
