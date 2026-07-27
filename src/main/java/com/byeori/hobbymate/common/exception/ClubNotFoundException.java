package com.byeori.hobbymate.common.exception;

public class ClubNotFoundException extends RuntimeException {

    public ClubNotFoundException() {
        super("존재하지 않거나 이용할 수 없는 모임입니다.");
    }
}
