package com.byeori.hobbymate.common.exception;

public class ClubNoticeCreationException extends RuntimeException {

    private final String fieldName;
    private final boolean accessDenied;
    private final Long clubId;

    public ClubNoticeCreationException(String fieldName, String message) {
        this(fieldName, message, false, null, null);
    }

    public ClubNoticeCreationException(String message, Throwable cause) {
        this(null, message, false, null, cause);
    }

    private ClubNoticeCreationException(
            String fieldName,
            String message,
            boolean accessDenied,
            Long clubId,
            Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
        this.accessDenied = accessDenied;
        this.clubId = clubId;
    }

    public static ClubNoticeCreationException accessDenied(Long clubId) {
        return new ClubNoticeCreationException(
                null,
                "모임장과 운영진만 공지사항을 작성할 수 있습니다.",
                true,
                clubId,
                null);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isAccessDenied() {
        return accessDenied;
    }

    public Long getClubId() {
        return clubId;
    }
}
