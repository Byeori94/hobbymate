package com.byeori.hobbymate.member.dto;

import com.byeori.hobbymate.common.validator.MemberValidationRules;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberWithdrawalRequest {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    @Size(max = MemberValidationRules.PASSWORD_MAX_LENGTH,
          message = "현재 비밀번호를 정확히 입력해 주세요.")
    private String currentPassword;

    @AssertTrue(message = "회원 탈퇴 안내를 확인하고 동의해 주세요.")
    private boolean withdrawAgreed;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public boolean isWithdrawAgreed() {
        return withdrawAgreed;
    }

    public void setWithdrawAgreed(boolean withdrawAgreed) {
        this.withdrawAgreed = withdrawAgreed;
    }

    public void clearPassword() {
        currentPassword = null;
    }
}
