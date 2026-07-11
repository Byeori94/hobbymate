# HobbyMate ERD

```mermaid
erDiagram
    HM_MEMBER ||--o{ HM_CLUB : creates
    HM_CATEGORY ||--o{ HM_CLUB : classifies
    HM_CATEGORY ||--o{ HM_CATEGORY : parent
    HM_CLUB ||--o{ HM_CLUB_MEMBER : contains
    HM_MEMBER ||--o{ HM_CLUB_MEMBER : joins
    HM_CLUB ||--o{ HM_CLUB_JOIN_REQUEST : receives
    HM_MEMBER ||--o{ HM_CLUB_JOIN_REQUEST : requests
    HM_CLUB ||--o{ HM_MEETING : holds
    HM_MEMBER ||--o{ HM_MEETING : creates
    HM_MEETING ||--o{ HM_MEETING_PARTICIPANT : has
    HM_MEMBER ||--o{ HM_MEETING_PARTICIPANT : participates
    HM_CLUB ||--o{ HM_CLUB_POST : owns
    HM_MEMBER ||--o{ HM_CLUB_POST : writes
    HM_MEETING o|--o{ HM_CLUB_POST : links
    HM_CLUB_POST ||--o{ HM_CLUB_POST_IMAGE : contains
    HM_CLUB_POST ||--o{ HM_CLUB_COMMENT : has
    HM_MEMBER ||--o{ HM_CLUB_COMMENT : writes
    HM_MEMBER ||--o{ HM_BOARD_POST : writes
    HM_BOARD_POST ||--o{ HM_BOARD_POST_IMAGE : contains
    HM_BOARD_POST ||--o{ HM_BOARD_COMMENT : has
    HM_MEMBER ||--o{ HM_BOARD_COMMENT : writes
    HM_MEMBER ||--o{ HM_SUGGESTION : submits
    HM_SUGGESTION ||--o{ HM_SUGGESTION_REPLY : receives
    HM_MEMBER ||--o{ HM_REPORT : submits
    HM_MEMBER ||--o{ HM_ADMIN_ACTION_HISTORY : performs
```

## 참고

- `HM_REPORT.TARGET_ID`와 `HM_ADMIN_ACTION_HISTORY.TARGET_ID`는 다형 대상이므로 물리 FK가 없다.
- `HM_CLUB_POST.MEETING_ID`는 만남 모집글·후기글에서만 사용하며 자유글에서는 NULL이다.
- 모임 내부 게시판과 만남 접근 권한은 `HM_CLUB_MEMBER` 기준으로 서비스 계층에서 검증한다.
