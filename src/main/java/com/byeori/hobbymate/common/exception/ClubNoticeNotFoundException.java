package com.byeori.hobbymate.common.exception;

public class ClubNoticeNotFoundException extends RuntimeException {

    public ClubNoticeNotFoundException() {
        super("존재하지 않거나 확인할 수 없는 공지사항입니다.");
    }
}
