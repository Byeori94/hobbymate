package com.byeori.hobbymate.category.vo;

public record CategoryCommand(
        Long adminMemberId,
        String categoryName,
        String description,
        Integer displayOrder,
        String useYn) {
}
