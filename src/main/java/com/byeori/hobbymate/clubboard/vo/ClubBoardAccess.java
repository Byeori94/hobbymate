package com.byeori.hobbymate.clubboard.vo;

public record ClubBoardAccess(
        Long clubId,
        String clubName,
        String memberRole,
        String memberStatus) {

    public boolean isActiveMember() {
        return "ACTIVE".equals(memberStatus);
    }

    public boolean canManageClub() {
        return isActiveMember()
                && ("LEADER".equals(memberRole) || "MANAGER".equals(memberRole));
    }
}
