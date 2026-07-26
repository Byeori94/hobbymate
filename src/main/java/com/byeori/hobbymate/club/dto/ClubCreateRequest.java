package com.byeori.hobbymate.club.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ClubCreateRequest {

    @NotBlank(message = "모임명을 입력해 주세요.")
    @Size(min = 2, max = 50, message = "모임명은 2자 이상 50자 이하로 입력해 주세요.")
    private String clubName;

    @NotNull(message = "카테고리를 선택해 주세요.")
    private Long categoryId;

    @NotBlank(message = "활동 지역을 입력해 주세요.")
    @Size(max = 200, message = "활동 지역은 200자 이하로 입력해 주세요.")
    private String activityRegion;

    @NotBlank(message = "한줄 소개를 입력해 주세요.")
    @Size(max = 500, message = "한줄 소개는 500자 이하로 입력해 주세요.")
    private String clubSummary;

    @NotBlank(message = "모임 소개를 입력해 주세요.")
    @Size(min = 10, max = 2000, message = "모임 소개는 10자 이상 2,000자 이하로 입력해 주세요.")
    private String clubDescription;

    @NotBlank(message = "성별 유형을 선택해 주세요.")
    @Pattern(regexp = "MIXED|FEMALE|MALE", message = "올바른 성별 유형을 선택해 주세요.")
    private String genderPolicy = "MIXED";

    @NotNull(message = "최소 연령을 입력해 주세요.")
    @Min(value = 18, message = "최소 연령은 만 18세 이상이어야 합니다.")
    @Max(value = 100, message = "최소 연령은 만 100세 이하여야 합니다.")
    private Integer minAge;

    @NotNull(message = "최대 연령을 입력해 주세요.")
    @Min(value = 18, message = "최대 연령은 만 18세 이상이어야 합니다.")
    @Max(value = 100, message = "최대 연령은 만 100세 이하여야 합니다.")
    private Integer maxAge;

    @NotNull(message = "최대 정원을 입력해 주세요.")
    @Min(value = 2, message = "최대 정원은 2명 이상이어야 합니다.")
    @Max(value = 1000, message = "최대 정원은 1,000명 이하여야 합니다.")
    private Integer maxMemberCount;

    @NotBlank(message = "가입 방식을 선택해 주세요.")
    @Pattern(regexp = "IMMEDIATE|APPROVAL", message = "올바른 가입 방식을 선택해 주세요.")
    private String joinType = "APPROVAL";

    @Size(max = 500, message = "가입 신청 안내는 500자 이하로 입력해 주세요.")
    private String joinGuide;

    private MultipartFile representativeImage;

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getActivityRegion() {
        return activityRegion;
    }

    public void setActivityRegion(String activityRegion) {
        this.activityRegion = activityRegion;
    }

    public String getClubSummary() {
        return clubSummary;
    }

    public void setClubSummary(String clubSummary) {
        this.clubSummary = clubSummary;
    }

    public String getClubDescription() {
        return clubDescription;
    }

    public void setClubDescription(String clubDescription) {
        this.clubDescription = clubDescription;
    }

    public String getGenderPolicy() {
        return genderPolicy;
    }

    public void setGenderPolicy(String genderPolicy) {
        this.genderPolicy = genderPolicy;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public void setMinAge(Integer minAge) {
        this.minAge = minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public Integer getMaxMemberCount() {
        return maxMemberCount;
    }

    public void setMaxMemberCount(Integer maxMemberCount) {
        this.maxMemberCount = maxMemberCount;
    }

    public String getJoinType() {
        return joinType;
    }

    public void setJoinType(String joinType) {
        this.joinType = joinType;
    }

    public String getJoinGuide() {
        return joinGuide;
    }

    public void setJoinGuide(String joinGuide) {
        this.joinGuide = joinGuide;
    }

    public MultipartFile getRepresentativeImage() {
        return representativeImage;
    }

    public void setRepresentativeImage(MultipartFile representativeImage) {
        this.representativeImage = representativeImage;
    }
}
