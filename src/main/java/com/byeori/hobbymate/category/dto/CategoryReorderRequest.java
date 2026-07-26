package com.byeori.hobbymate.category.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryReorderRequest {

    private List<Long> categoryIds = new ArrayList<>();
    private List<String> displayOrders = new ArrayList<>();

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<String> getDisplayOrders() {
        return displayOrders;
    }

    public void setDisplayOrders(List<String> displayOrders) {
        this.displayOrders = displayOrders;
    }
}
