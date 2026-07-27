package com.byeori.hobbymate.clubboard.dto;

import com.byeori.hobbymate.clubboard.vo.ClubNoticeAdjacentPost;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeDetail;

public record ClubNoticeDetailView(
        Long clubId,
        String clubName,
        boolean canManageClub,
        boolean canEditNotice,
        boolean canDeleteNotice,
        ClubNoticeDetail notice,
        ClubNoticeAdjacentPost previousNotice,
        ClubNoticeAdjacentPost nextNotice,
        ClubNoticeReturnQuery returnQuery) {
}
