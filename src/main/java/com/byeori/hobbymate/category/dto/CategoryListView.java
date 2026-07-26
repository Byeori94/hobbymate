package com.byeori.hobbymate.category.dto;

import java.util.List;

import com.byeori.hobbymate.category.vo.AdminCategory;

public record CategoryListView(
        List<AdminCategory> categories,
        CategorySearchCondition search,
        boolean reorderAvailable) {

    public CategoryListView {
        categories = List.copyOf(categories);
    }
}
