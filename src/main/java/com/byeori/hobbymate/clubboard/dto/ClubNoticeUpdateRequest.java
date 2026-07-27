package com.byeori.hobbymate.clubboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClubNoticeUpdateRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 200, message = "제목은 2자 이상 200자 이하로 입력해 주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(min = 10, max = 10000, message = "내용은 10자 이상 10,000자 이하로 입력해 주세요.")
    private String content;

    @Pattern(regexp = "Y|N", message = "상단 고정 값이 올바르지 않습니다.")
    private String pinnedYn = "N";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }

    public String getPinnedYn() {
        return pinnedYn;
    }

    public void setPinnedYn(String pinnedYn) {
        this.pinnedYn = pinnedYn;
    }
}
