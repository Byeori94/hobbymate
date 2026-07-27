package com.byeori.hobbymate.common.exception;

public class ClubNoticeDeletionException extends RuntimeException {

    private final Long clubId;
    private final Long postId;

    public ClubNoticeDeletionException(
            Long clubId,
            Long postId,
            String message,
            Throwable cause) {
        super(message, cause);
        this.clubId = clubId;
        this.postId = postId;
    }

    public Long getClubId() {
        return clubId;
    }

    public Long getPostId() {
        return postId;
    }
}
