package com.byeori.hobbymate.club.vo;

import java.time.LocalDateTime;

public record ClubMemberRelation(
        String memberRole,
        String memberStatus,
        String pendingJoinYn,
        String permanentBanYn,
        LocalDateTime rejoinAvailableAt) {

    public boolean isActiveMember() {
        return "ACTIVE".equals(memberStatus);
    }

    public boolean isPendingJoin() {
        return "Y".equals(pendingJoinYn);
    }

    public boolean isPermanentlyBanned() {
        return "Y".equals(permanentBanYn);
    }
}
