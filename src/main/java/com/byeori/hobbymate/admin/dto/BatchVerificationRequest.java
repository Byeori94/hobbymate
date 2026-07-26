package com.byeori.hobbymate.admin.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BatchVerificationRequest {

    private List<Long> memberIds = new ArrayList<>();

    @NotBlank(message = "인증 처리 사유를 입력해주세요.")
    @Size(max = 2000, message = "인증 처리 사유는 2000자 이하로 입력해주세요.")
    private String reason;

    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds == null ? new ArrayList<>() : memberIds;
    }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
