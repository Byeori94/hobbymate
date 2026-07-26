package com.byeori.hobbymate.club.vo;

public class ClubCreation {

    private Long clubId;
    private final Long ownerMemberId;
    private final Long categoryId;
    private final String clubName;
    private final String clubSummary;
    private final String clubDescription;
    private final String representativeImageUrl;
    private final String activityRegion;
    private final String genderPolicy;
    private final Integer minAge;
    private final Integer maxAge;
    private final Integer maxMemberCount;
    private final String joinType;
    private final String joinGuide;

    public ClubCreation(
            Long ownerMemberId,
            Long categoryId,
            String clubName,
            String clubSummary,
            String clubDescription,
            String representativeImageUrl,
            String activityRegion,
            String genderPolicy,
            Integer minAge,
            Integer maxAge,
            Integer maxMemberCount,
            String joinType,
            String joinGuide) {
        this.ownerMemberId = ownerMemberId;
        this.categoryId = categoryId;
        this.clubName = clubName;
        this.clubSummary = clubSummary;
        this.clubDescription = clubDescription;
        this.representativeImageUrl = representativeImageUrl;
        this.activityRegion = activityRegion;
        this.genderPolicy = genderPolicy;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.maxMemberCount = maxMemberCount;
        this.joinType = joinType;
        this.joinGuide = joinGuide;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public Long getOwnerMemberId() {
        return ownerMemberId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getClubName() {
        return clubName;
    }

    public String getClubSummary() {
        return clubSummary;
    }

    public String getClubDescription() {
        return clubDescription;
    }

    public String getRepresentativeImageUrl() {
        return representativeImageUrl;
    }

    public String getActivityRegion() {
        return activityRegion;
    }

    public String getGenderPolicy() {
        return genderPolicy;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public Integer getMaxMemberCount() {
        return maxMemberCount;
    }

    public String getJoinType() {
        return joinType;
    }

    public String getJoinGuide() {
        return joinGuide;
    }
}
