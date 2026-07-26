package com.byeori.hobbymate.category.vo;

import java.time.LocalDateTime;

public record AdminCategory(
        Long categoryId,
        String categoryName,
        String description,
        Integer displayOrder,
        String useYn,
        long clubCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public boolean isUsed() {
        return "Y".equals(useYn);
    }

    public String useLabel() {
        return isUsed() ? "사용 중" : "사용 중지";
    }
}
