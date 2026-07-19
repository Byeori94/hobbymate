package com.byeori.hobbymate.common.exception;

public class MemberWithdrawalException extends RuntimeException {

    private final String field;

    public MemberWithdrawalException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
