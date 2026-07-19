package com.byeori.hobbymate.member.dto;

import com.byeori.hobbymate.common.validator.MemberValidationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberPasswordChangeRequest {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    @Size(max = MemberValidationRules.PASSWORD_MAX_LENGTH,
          message = "현재 비밀번호를 정확히 입력해 주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Size(min = MemberValidationRules.PASSWORD_MIN_LENGTH,
          max = MemberValidationRules.PASSWORD_MAX_LENGTH,
          message = "새 비밀번호는 8자 이상 255자 이하로 입력해 주세요.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
    @Size(max = MemberValidationRules.PASSWORD_MAX_LENGTH,
          message = "새 비밀번호 확인은 255자 이하로 입력해 주세요.")
    private String newPasswordConfirm;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm() {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm(String newPasswordConfirm) {
        this.newPasswordConfirm = newPasswordConfirm;
    }

    public void clearPasswords() {
        currentPassword = null;
        newPassword = null;
        newPasswordConfirm = null;
    }
}
