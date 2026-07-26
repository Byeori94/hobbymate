package com.byeori.hobbymate.admin.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TemporaryVerificationRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
    private String name;

    @NotNull(message = "생년월일을 입력해주세요.")
    @Past(message = "생년월일을 확인해주세요.")
    private LocalDate birthDate;

    @NotBlank(message = "성별을 선택해주세요.")
    @Pattern(regexp = "FEMALE|MALE", message = "성별을 확인해주세요.")
    private String gender;

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    private String phone;

    @NotBlank(message = "인증 처리 사유를 입력해주세요.")
    @Size(max = 2000, message = "인증 처리 사유는 2000자 이하로 입력해주세요.")
    private String reason;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
