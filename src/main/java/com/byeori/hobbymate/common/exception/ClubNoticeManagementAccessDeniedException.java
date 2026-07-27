package com.byeori.hobbymate.common.exception;

public class ClubNoticeManagementAccessDeniedException extends RuntimeException {

    private final Long clubId;

    public ClubNoticeManagementAccessDeniedException(Long clubId) {
        super("모임장과 운영진만 공지사항을 수정하거나 삭제할 수 있습니다.");
        this.clubId = clubId;
    }

    public Long getClubId() {
        return clubId;
    }
}
