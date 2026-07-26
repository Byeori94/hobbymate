package com.byeori.hobbymate.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerificationCancelRequest {

    @NotBlank(message = "취소 사유를 입력해주세요.")
    @Size(max = 2000, message = "취소 사유는 2000자 이하로 입력해주세요.")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
