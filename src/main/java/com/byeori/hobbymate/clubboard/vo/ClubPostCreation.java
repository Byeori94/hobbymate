package com.byeori.hobbymate.clubboard.vo;

public class ClubPostCreation {

    private Long postId;
    private final Long clubId;
    private final Long authorMemberId;
    private final String postType;
    private final String title;
    private final String content;
    private final String pinnedYn;

    public ClubPostCreation(
            Long clubId,
            Long authorMemberId,
            String postType,
            String title,
            String content,
            String pinnedYn) {
        this.clubId = clubId;
        this.authorMemberId = authorMemberId;
        this.postType = postType;
        this.title = title;
        this.content = content;
        this.pinnedYn = pinnedYn;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getClubId() {
        return clubId;
    }

    public Long getAuthorMemberId() {
        return authorMemberId;
    }

    public String getPostType() {
        return postType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getPinnedYn() {
        return pinnedYn;
    }
}
