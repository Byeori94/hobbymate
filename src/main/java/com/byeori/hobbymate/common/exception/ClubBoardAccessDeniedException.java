package com.byeori.hobbymate.common.exception;

public class ClubBoardAccessDeniedException extends RuntimeException {

    private final Long clubId;

    public ClubBoardAccessDeniedException(Long clubId) {
        super("모임에 가입한 회원만 공지사항을 확인할 수 있습니다.");
        this.clubId = clubId;
    }

    public Long getClubId() {
        return clubId;
    }
}
