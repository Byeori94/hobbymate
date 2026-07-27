package com.byeori.hobbymate.clubboard.vo;

public record ClubPostUpdate(
        Long clubId,
        Long postId,
        String title,
        String content,
        String pinnedYn) {
}
