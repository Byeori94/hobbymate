package com.byeori.hobbymate.club.vo;

import java.time.LocalDateTime;

public record ClubDetail(
        Long clubId,
        String representativeImageUrl,
        String clubName,
        String categoryName,
        String activityRegion,
        String clubSummary,
        String clubDescription,
        String genderPolicy,
        Integer minAge,
        Integer maxAge,
        int memberCount,
        int maxMemberCount,
        String recruitStatus,
        String joinType,
        String joinGuide,
        Long leaderMemberId,
        String leaderNickname,
        String leaderProfileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastActivityAt) {

    public boolean hasRepresentativeImage() {
        return representativeImageUrl != null && !representativeImageUrl.isBlank();
    }

    public boolean hasLeaderProfileImage() {
        return leaderProfileImageUrl != null && !leaderProfileImageUrl.isBlank();
    }

    public boolean hasJoinGuide() {
        return joinGuide != null && !joinGuide.isBlank();
    }

    public String categoryDisplayName() {
        return categoryName == null || categoryName.isBlank() ? "카테고리 미정" : categoryName;
    }

    public String regionDisplayName() {
        return activityRegion == null || activityRegion.isBlank() ? "활동 지역 미정" : activityRegion;
    }

    public String leaderDisplayName() {
        return leaderNickname == null || leaderNickname.isBlank() ? "알 수 없음" : leaderNickname;
    }

    public String genderLabel() {
        return switch (genderPolicy == null ? "" : genderPolicy) {
            case "MIXED" -> "성별 제한 없음";
            case "FEMALE" -> "여성만 가입 가능";
            case "MALE" -> "남성만 가입 가능";
            default -> "가입 성별 조건 미정";
        };
    }

    public String ageLabel() {
        if (minAge == null && maxAge == null) {
            return "연령 제한 없음";
        }
        if (minAge == null) {
            return "만 " + maxAge + "세 이하";
        }
        if (maxAge == null) {
            return "만 " + minAge + "세 이상";
        }
        return "만 " + minAge + "세~만 " + maxAge + "세";
    }

    public String recruitStatusLabel() {
        return switch (recruitStatus == null ? "" : recruitStatus) {
            case "OPEN" -> "모집 중";
            case "FULL" -> "정원 마감";
            case "CLOSED" -> "모집 종료";
            default -> "모집 상태 미정";
        };
    }

    public String joinTypeLabel() {
        return switch (joinType == null ? "" : joinType) {
            case "IMMEDIATE" -> "즉시 가입";
            case "APPROVAL" -> "승인 후 가입";
            default -> "가입 방식 미정";
        };
    }

    public String joinTypeDescription() {
        return switch (joinType == null ? "" : joinType) {
            case "IMMEDIATE" -> "가입 조건을 충족하면 바로 모임 회원이 됩니다.";
            case "APPROVAL" -> "가입 신청 후 모임장 또는 운영진의 승인이 필요합니다.";
            default -> "가입 방식이 정해지지 않았습니다.";
        };
    }

    public LocalDateTime displayActivityAt() {
        if (lastActivityAt != null) {
            return lastActivityAt;
        }
        return updatedAt != null ? updatedAt : createdAt;
    }
}
