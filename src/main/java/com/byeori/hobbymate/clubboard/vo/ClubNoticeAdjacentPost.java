package com.byeori.hobbymate.clubboard.vo;

import java.time.LocalDateTime;

public record ClubNoticeAdjacentPost(
        Long postId,
        String title,
        LocalDateTime createdAt) {
}
