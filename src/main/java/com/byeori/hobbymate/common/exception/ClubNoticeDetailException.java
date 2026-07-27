package com.byeori.hobbymate.common.exception;

public class ClubNoticeDetailException extends RuntimeException {

    private final Long clubId;

    public ClubNoticeDetailException(Long clubId, Throwable cause) {
        super("공지사항을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.", cause);
        this.clubId = clubId;
    }

    public Long getClubId() {
        return clubId;
    }
}
