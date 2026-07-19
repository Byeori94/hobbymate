package com.byeori.hobbymate.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.byeori.hobbymate.common.validator.MemberValidationRules;

public class MemberJoinRequest {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(regexp = "^[a-z][a-z0-9]{4,19}$", message = "아이디 형식을 확인해 주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = MemberValidationRules.PASSWORD_MIN_LENGTH,
          max = MemberValidationRules.PASSWORD_MAX_LENGTH,
          message = "비밀번호는 8자 이상 255자 이하로 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
    @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "이름 형식을 확인해 주세요.")
    private String name;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(max = 50, message = "닉네임은 50자 이하로 입력해 주세요.")
    private String nickname;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "이메일 형식을 확인해 주세요.")
    @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
    private String email;

    @NotBlank(message = "휴대폰 번호를 입력해 주세요.")
    @Size(max = 30, message = "휴대폰 번호를 정확히 입력해 주세요.")
    private String phone;

    @NotNull(message = "생년월일을 입력해 주세요.")
    @Past(message = "생년월일을 정확히 입력해 주세요.")
    private LocalDate birthDate;

    @NotBlank(message = "성별을 선택해 주세요.")
    @Pattern(regexp = "FEMALE|MALE", message = "성별을 정확히 선택해 주세요.")
    private String gender;

    @AssertTrue(message = "이용약관에 동의해 주세요.")
    private boolean termsAgreed;

    @AssertTrue(message = "개인정보 수집 및 이용에 동의해 주세요.")
    private boolean privacyAgreed;

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public boolean isTermsAgreed() { return termsAgreed; }
    public void setTermsAgreed(boolean termsAgreed) { this.termsAgreed = termsAgreed; }
    public boolean isPrivacyAgreed() { return privacyAgreed; }
    public void setPrivacyAgreed(boolean privacyAgreed) { this.privacyAgreed = privacyAgreed; }
    public void clearPasswords() { password = null; passwordConfirm = null; }
}
