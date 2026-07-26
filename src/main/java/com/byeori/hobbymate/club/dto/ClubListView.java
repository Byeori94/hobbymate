package com.byeori.hobbymate.club.dto;

import java.util.List;

import com.byeori.hobbymate.club.vo.ClubCategory;
import com.byeori.hobbymate.club.vo.ClubListItem;

public record ClubListView(
        ClubPage<ClubListItem> clubs,
        ClubSearchCondition search,
        List<ClubCategory> categories,
        List<String> validationMessages) {

    public ClubListView {
        categories = List.copyOf(categories);
        validationMessages = List.copyOf(validationMessages);
    }
}
