package com.byeori.hobbymate.admin.service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.admin.dao.AdminMemberVerificationDao;
import com.byeori.hobbymate.admin.dto.AdminPage;
import com.byeori.hobbymate.admin.dto.BatchVerificationCancelRequest;
import com.byeori.hobbymate.admin.dto.BatchVerificationRequest;
import com.byeori.hobbymate.admin.dto.TemporaryVerificationRequest;
import com.byeori.hobbymate.admin.dto.VerificationCancelRequest;
import com.byeori.hobbymate.admin.dto.VerificationListView;
import com.byeori.hobbymate.admin.dto.VerificationSearchCondition;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationRow;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationState;
import com.byeori.hobbymate.admin.vo.AdminVerificationHistory;
import com.byeori.hobbymate.common.exception.AdminVerificationException;

@Service
public class AdminMemberVerificationService {

    private static final String ADMIN_TEMP = "ADMIN_TEMP";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^010\\d{8}$");

    private final AdminMemberVerificationDao dao;

    public AdminMemberVerificationService(AdminMemberVerificationDao dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public VerificationListView getList(
            VerificationSearchCondition search,
            int authPage,
            int cancelPage) {
        long authCount = dao.countAuthTargets(search);
        int normalizedAuthPage = AdminPage.normalizePage(authPage, authCount);
        List<AdminMemberVerificationRow> authRows = dao.findAuthTargets(
                search,
                (normalizedAuthPage - 1) * AdminPage.PAGE_SIZE,
                AdminPage.PAGE_SIZE);

        long cancelCount = dao.countCancelTargets(search);
        int normalizedCancelPage = AdminPage.normalizePage(cancelPage, cancelCount);
        List<AdminMemberVerificationRow> cancelRows = dao.findCancelTargets(
                search,
                (normalizedCancelPage - 1) * AdminPage.PAGE_SIZE,
                AdminPage.PAGE_SIZE);

        return new VerificationListView(
                AdminPage.of(authRows, authCount, normalizedAuthPage),
                AdminPage.of(cancelRows, cancelCount, normalizedCancelPage),
                search);
    }

    @Transactional(readOnly = true)
    public AdminMemberVerificationState getProcessingTarget(Long memberId) {
        AdminMemberVerificationState member = requireMember(dao.findMemberState(memberId));
        validateProcessingTarget(member);
        return member;
    }

    @Transactional(readOnly = true)
    public AdminMemberVerificationState getCancelTarget(Long memberId) {
        AdminMemberVerificationState member = requireMember(dao.findMemberState(memberId));
        validateCancelTarget(member);
        return member;
    }

    @Transactional
    public int processTemporary(
            Long adminMemberId,
            Long memberId,
            TemporaryVerificationRequest request) {
        requireAdmin(adminMemberId);
        String name = trim(request.getName());
        String phone = normalizePhone(request.getPhone());
        String reason = requireReason(request.getReason(), "인증 처리 사유를 입력해주세요.");
        validateIdentityFields(name, request.getBirthDate(), request.getGender(), phone, null);

        AdminMemberVerificationState member =
                requireMember(dao.findMemberStateForUpdate(memberId));
        validateProcessingTarget(member);
        String ciHash = createTemporaryCi(member.memberId());
        updateTemporary(member, name, request.getBirthDate(), request.getGender(), phone,
                ciHash, reason, adminMemberId, "INDIVIDUAL", UUID.randomUUID().toString());
        return 1;
    }

    @Transactional
    public int processTemporaryBatch(Long adminMemberId, BatchVerificationRequest request) {
        requireAdmin(adminMemberId);
        List<Long> memberIds = normalizeIds(request.getMemberIds(),
                "임시 본인인증 처리할 회원을 선택해주세요.");
        String reason = requireReason(request.getReason(), "인증 처리 사유를 입력해주세요.");
        List<AdminMemberVerificationState> members = memberIds.stream()
                .map(id -> requireMember(dao.findMemberStateForUpdate(id)))
                .toList();

        for (AdminMemberVerificationState member : members) {
            validateProcessingTarget(member);
            validateIdentityFields(
                    trim(member.name()),
                    member.birthDate(),
                    member.gender(),
                    normalizePhone(member.phone()),
                    member.loginId());
        }

        String operationId = UUID.randomUUID().toString();
        for (AdminMemberVerificationState member : members) {
            updateTemporary(
                    member,
                    trim(member.name()),
                    member.birthDate(),
                    member.gender(),
                    normalizePhone(member.phone()),
                    createTemporaryCi(member.memberId()),
                    reason,
                    adminMemberId,
                    "BATCH",
                    operationId);
        }
        return members.size();
    }

    @Transactional
    public int cancelTemporary(
            Long adminMemberId,
            Long memberId,
            VerificationCancelRequest request) {
        requireAdmin(adminMemberId);
        String reason = requireReason(request.getReason(), "취소 사유를 입력해주세요.");
        AdminMemberVerificationState member =
                requireMember(dao.findMemberStateForUpdate(memberId));
        validateCancelTarget(member);
        cancel(member, reason, adminMemberId, "INDIVIDUAL", UUID.randomUUID().toString());
        return 1;
    }

    @Transactional
    public int cancelTemporaryBatch(
            Long adminMemberId,
            BatchVerificationCancelRequest request) {
        requireAdmin(adminMemberId);
        List<Long> memberIds = normalizeIds(request.getMemberIds(),
                "임시 본인인증을 취소할 회원을 선택해주세요.");
        String reason = requireReason(request.getReason(), "취소 사유를 입력해주세요.");
        List<AdminMemberVerificationState> members = memberIds.stream()
                .map(id -> requireMember(dao.findMemberStateForUpdate(id)))
                .toList();
        members.forEach(this::validateCancelTarget);

        String operationId = UUID.randomUUID().toString();
        for (AdminMemberVerificationState member : members) {
            cancel(member, reason, adminMemberId, "BATCH", operationId);
        }
        return members.size();
    }

    private void updateTemporary(
            AdminMemberVerificationState before,
            String name,
            LocalDate birthDate,
            String gender,
            String phone,
            String ciHash,
            String reason,
            Long adminMemberId,
            String processingType,
            String operationId) {
        int updated = dao.updateTemporaryVerification(
                before.memberId(), name, birthDate, gender, phone, ciHash, reason, adminMemberId);
        if (updated != 1) {
            throw changedState();
        }
        int inserted = dao.insertVerificationHistory(new AdminVerificationHistory(
                adminMemberId,
                before.memberId(),
                "ADMIN_TEMP_AUTH",
                reason,
                before.identityVerifiedYn(),
                "Y",
                before.verificationMethod(),
                ADMIN_TEMP,
                before.ciHash(),
                ciHash,
                processingType,
                operationId));
        if (inserted != 1) {
            throw new AdminVerificationException("인증 처리 이력을 저장할 수 없습니다.");
        }
    }

    private void cancel(
            AdminMemberVerificationState before,
            String reason,
            Long adminMemberId,
            String processingType,
            String operationId) {
        if (dao.cancelTemporaryVerification(before.memberId(), adminMemberId) != 1) {
            throw changedState();
        }
        if (dao.insertVerificationHistory(new AdminVerificationHistory(
                adminMemberId,
                before.memberId(),
                "ADMIN_TEMP_AUTH_CANCEL",
                reason,
                before.identityVerifiedYn(),
                "N",
                before.verificationMethod(),
                null,
                before.ciHash(),
                null,
                processingType,
                operationId)) != 1) {
            throw new AdminVerificationException("인증 취소 이력을 저장할 수 없습니다.");
        }
    }

    private void requireAdmin(Long adminMemberId) {
        if (adminMemberId == null || !dao.isAdminMember(adminMemberId)) {
            throw new AdminVerificationException("관리자 권한이 필요합니다.");
        }
    }

    private AdminMemberVerificationState requireMember(AdminMemberVerificationState member) {
        if (member == null) {
            throw new AdminVerificationException("대상 회원을 찾을 수 없습니다.");
        }
        return member;
    }

    private void validateProcessingTarget(AdminMemberVerificationState member) {
        validateGeneralTarget(member);
        if (!"N".equals(member.identityVerifiedYn())) {
            throw new AdminVerificationException("이미 본인인증이 완료된 회원입니다.");
        }
        if (member.verificationMethod() != null && !member.verificationMethod().isBlank()) {
            throw changedState();
        }
    }

    private void validateCancelTarget(AdminMemberVerificationState member) {
        validateGeneralTarget(member);
        if (!"Y".equals(member.identityVerifiedYn())) {
            throw new AdminVerificationException("미인증 회원의 인증을 취소할 수 없습니다.");
        }
        if (!ADMIN_TEMP.equals(member.verificationMethod())) {
            throw new AdminVerificationException(
                    "실제 본인인증이 완료된 회원은 관리자 화면에서 취소할 수 없습니다.");
        }
    }

    private void validateGeneralTarget(AdminMemberVerificationState member) {
        if (!"USER".equals(member.memberRole())) {
            throw new AdminVerificationException("관리자 계정은 임시 본인인증 처리 대상이 아닙니다.");
        }
        if ("WITHDRAWN".equals(member.memberStatus())) {
            throw new AdminVerificationException("탈퇴한 회원은 임시 본인인증 처리할 수 없습니다.");
        }
    }

    private void validateIdentityFields(
            String name,
            LocalDate birthDate,
            String gender,
            String phone,
            String loginId) {
        String prefix = loginId == null ? "" : loginId + " 회원의 ";
        if (name == null || name.isBlank() || name.length() > 50) {
            throw new AdminVerificationException(prefix + "이름 정보가 없어 임시 본인인증을 처리할 수 없습니다.");
        }
        if (birthDate == null || !birthDate.isBefore(LocalDate.now())) {
            throw new AdminVerificationException(prefix + "생년월일 정보가 없어 임시 본인인증을 처리할 수 없습니다.");
        }
        if (!"FEMALE".equals(gender) && !"MALE".equals(gender)) {
            throw new AdminVerificationException(prefix + "성별 정보가 없어 임시 본인인증을 처리할 수 없습니다.");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new AdminVerificationException(prefix + "휴대폰 번호 형식이 올바르지 않아 임시 본인인증을 처리할 수 없습니다.");
        }
    }

    private List<Long> normalizeIds(List<Long> ids, String emptyMessage) {
        if (ids == null) {
            throw new AdminVerificationException(emptyMessage);
        }
        List<Long> normalized = new LinkedHashSet<>(ids).stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (normalized.isEmpty()) {
            throw new AdminVerificationException(emptyMessage);
        }
        return normalized;
    }

    private String requireReason(String value, String message) {
        String reason = trim(value);
        if (reason == null || reason.isBlank()) {
            throw new AdminVerificationException(message);
        }
        if (reason.length() > 2000) {
            throw new AdminVerificationException("처리 사유는 2000자 이하로 입력해주세요.");
        }
        return reason;
    }

    private String normalizePhone(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.replaceAll("[\\s-]", "");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String createTemporaryCi(Long memberId) {
        return "TEMP-" + memberId + "-" + UUID.randomUUID();
    }

    private AdminVerificationException changedState() {
        return new AdminVerificationException(
                "회원정보가 변경되어 요청을 처리할 수 없습니다. 목록을 새로고침한 뒤 다시 시도해주세요.");
    }
}
