package com.byeori.hobbymate.club.dto;

import com.byeori.hobbymate.club.vo.ClubDetail;

public record ClubDetailView(
        ClubDetail club,
        String relationshipType,
        String relationshipLabel,
        String actionLabel,
        String actionMessage) {

    public boolean isAnonymous() {
        return "ANONYMOUS".equals(relationshipType);
    }

    public boolean isNonMember() {
        return "NON_MEMBER".equals(relationshipType);
    }

    public boolean canManageClub() {
        return "LEADER".equals(relationshipType) || "MANAGER".equals(relationshipType);
    }
}
