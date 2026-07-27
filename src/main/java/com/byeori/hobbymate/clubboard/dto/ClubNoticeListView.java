package com.byeori.hobbymate.clubboard.dto;

import java.util.List;

import com.byeori.hobbymate.club.dto.ClubPage;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;

public record ClubNoticeListView(
        Long clubId,
        String clubName,
        String memberRole,
        boolean canManageClub,
        boolean canWriteNotice,
        ClubPage<ClubNoticeListItem> notices,
        ClubNoticeSearchCondition search,
        List<String> validationMessages) {

    public ClubNoticeListView {
        validationMessages = List.copyOf(validationMessages);
    }
}
