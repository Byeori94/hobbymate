package com.byeori.hobbymate.club.dto;

import java.util.List;

import com.byeori.hobbymate.club.vo.ClubCategory;

public record ClubCreationPage(List<ClubCategory> categories) {

    public ClubCreationPage {
        categories = List.copyOf(categories);
    }
}
