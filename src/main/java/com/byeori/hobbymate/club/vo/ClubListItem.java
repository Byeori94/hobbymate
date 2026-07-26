package com.byeori.hobbymate.club.vo;

import java.time.LocalDateTime;

public record ClubListItem(
        Long clubId,
        String representativeImageUrl,
        String clubName,
        String categoryName,
        String activityRegion,
        String clubSummary,
        String genderPolicy,
        Integer minAge,
        Integer maxAge,
        int memberCount,
        int maxMemberCount,
        String recruitStatus,
        String joinType,
        String leaderNickname,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt) {

    public boolean hasRepresentativeImage() {
        return representativeImageUrl != null && !representativeImageUrl.isBlank();
    }

    public String genderLabel() {
        return switch (genderPolicy == null ? "" : genderPolicy) {
            case "MIXED" -> "혼성";
            case "FEMALE" -> "여성";
            case "MALE" -> "남성";
            default -> "-";
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
        return "만 " + minAge + "–" + maxAge + "세";
    }

    public String recruitStatusLabel() {
        return switch (recruitStatus == null ? "" : recruitStatus) {
            case "OPEN" -> "모집 중";
            case "FULL" -> "정원 마감";
            case "CLOSED" -> "모집 종료";
            default -> "-";
        };
    }

    public String joinTypeLabel() {
        return switch (joinType == null ? "" : joinType) {
            case "IMMEDIATE" -> "즉시 가입";
            case "APPROVAL" -> "승인 후 가입";
            default -> "-";
        };
    }

    public String leaderDisplayName() {
        return leaderNickname == null || leaderNickname.isBlank() ? "알 수 없음" : leaderNickname;
    }
}
