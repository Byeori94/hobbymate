package com.byeori.hobbymate.admin.dto;

import com.byeori.hobbymate.admin.vo.AdminMemberVerificationRow;

public record VerificationListView(
        AdminPage<AdminMemberVerificationRow> authTargets,
        AdminPage<AdminMemberVerificationRow> cancelTargets,
        VerificationSearchCondition search) {
}
