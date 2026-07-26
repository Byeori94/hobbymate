package com.byeori.hobbymate.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "카테고리명을 입력해주세요.")
    @Size(min = 2, max = 50, message = "카테고리명은 2자 이상 50자 이하로 입력해주세요.")
    private String categoryName;

    @Size(max = 200, message = "설명은 200자 이하로 입력해주세요.")
    private String description;

    @NotNull(message = "노출 순서를 입력해주세요.")
    @Min(value = 1, message = "노출 순서는 1 이상의 정수로 입력해주세요.")
    private Integer displayOrder;

    @NotBlank(message = "사용 여부를 선택해주세요.")
    @Pattern(regexp = "Y|N", message = "올바른 사용 여부를 선택해주세요.")
    private String useYn = "Y";

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }
}
