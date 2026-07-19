package com.byeori.hobbymate.member.service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.common.exception.MemberProfileUpdateException;
import com.byeori.hobbymate.common.exception.MemberPasswordChangeException;
import com.byeori.hobbymate.common.exception.MemberWithdrawalException;
import com.byeori.hobbymate.common.validator.MemberValidationRules;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.dto.MemberPasswordChangeRequest;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateRequest;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateResult;
import com.byeori.hobbymate.member.dto.MemberWithdrawalRequest;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;
import com.byeori.hobbymate.member.vo.MemberProfileUpdate;

@Service
public class MemberMyPageService {

    private static final String EMPTY_VALUE = "-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    public MemberMyPageService(MemberDao memberDao, PasswordEncoder passwordEncoder) {
        this.memberDao = memberDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<MemberMyPageResponse> getMyPage(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(memberDao.findActiveMemberForMyPage(memberId))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(Long memberId, String nickname) {
        MemberMyPageInfo current = requireActiveMember(memberId);
        String normalized = MemberValidationRules.normalizeNickname(nickname);
        validateNickname(normalized);

        return current.nickname().equals(normalized)
                || !memberDao.existsNicknameExceptMember(normalized, memberId);
    }

    @Transactional
    public MemberProfileUpdateResult updateProfile(
            Long memberId,
            MemberProfileUpdateRequest request) {
        MemberMyPageInfo current = requireActiveMember(memberId);
        String nickname = MemberValidationRules.normalizeNickname(request.getNickname());
        String email = MemberValidationRules.normalizeEmail(request.getEmail());
        validateNickname(nickname);
        validateEmail(email);

        boolean nicknameChanged = !current.nickname().equals(nickname);
        boolean emailChanged = !current.email().equals(email);
        if (nicknameChanged && memberDao.existsNicknameExceptMember(nickname, memberId)) {
            throw new MemberProfileUpdateException("nickname", "이미 사용 중인 닉네임입니다.");
        }
        if (!nicknameChanged && !emailChanged) {
            return new MemberProfileUpdateResult(current.nickname(), false);
        }

        try {
            int updatedRows = memberDao.updateActiveMemberProfile(
                    new MemberProfileUpdate(memberId, nickname, email));
            if (updatedRows != 1) {
                throw new MemberProfileUpdateException(null, "회원정보를 수정할 수 없습니다.");
            }
        } catch (DataIntegrityViolationException ex) {
            if (memberDao.existsNicknameExceptMember(nickname, memberId)) {
                throw new MemberProfileUpdateException("nickname", "이미 사용 중인 닉네임입니다.");
            }
            throw new MemberProfileUpdateException("email", "입력한 이메일을 사용할 수 없습니다.");
        }

        request.setNickname(nickname);
        request.setEmail(email);
        return new MemberProfileUpdateResult(nickname, true);
    }

    @Transactional
    public void changePassword(Long memberId, MemberPasswordChangeRequest request) {
        if (memberId == null) {
            throw new MemberPasswordChangeException(null, "회원정보를 찾을 수 없습니다.");
        }
        if (request.getNewPassword() == null
                || !request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new MemberPasswordChangeException(
                    "newPasswordConfirm", "새 비밀번호와 확인값이 일치하지 않습니다.");
        }
        String storedPasswordHash = memberDao.findActivePasswordHashByMemberId(memberId);
        if (storedPasswordHash == null || storedPasswordHash.isBlank()) {
            throw new MemberPasswordChangeException(null, "회원정보를 찾을 수 없습니다.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), storedPasswordHash)) {
            throw new MemberPasswordChangeException(
                    "currentPassword", "현재 비밀번호가 일치하지 않습니다.");
        }
        if (!MemberValidationRules.isValidPassword(request.getNewPassword())) {
            throw new MemberPasswordChangeException(
                    "newPassword", "새 비밀번호 형식이 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), storedPasswordHash)) {
            throw new MemberPasswordChangeException(
                    "newPassword", "새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        if (memberDao.updateActiveMemberPassword(memberId, encodedPassword) != 1) {
            throw new MemberPasswordChangeException(null, "비밀번호를 변경할 수 없습니다.");
        }
    }

    @Transactional
    public void withdrawMember(Long memberId, MemberWithdrawalRequest request) {
        if (memberId == null) {
            throw new MemberWithdrawalException(null, "회원정보를 찾을 수 없습니다.");
        }
        if (!request.isWithdrawAgreed()) {
            throw new MemberWithdrawalException(
                    "withdrawAgreed", "회원 탈퇴 안내를 확인하고 동의해 주세요.");
        }

        String storedPasswordHash = memberDao.findActivePasswordHashByMemberId(memberId);
        if (storedPasswordHash == null || storedPasswordHash.isBlank()) {
            throw new MemberWithdrawalException(null, "회원정보를 찾을 수 없습니다.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), storedPasswordHash)) {
            throw new MemberWithdrawalException(
                    "currentPassword", "현재 비밀번호가 일치하지 않습니다.");
        }
        if (memberDao.withdrawActiveMember(memberId) != 1) {
            throw new MemberWithdrawalException(null, "회원 탈퇴를 처리할 수 없습니다.");
        }
    }

    private MemberMyPageInfo requireActiveMember(Long memberId) {
        if (memberId == null) {
            throw new MemberProfileUpdateException(null, "회원정보를 찾을 수 없습니다.");
        }
        MemberMyPageInfo member = memberDao.findActiveMemberForMyPage(memberId);
        if (member == null) {
            throw new MemberProfileUpdateException(null, "회원정보를 찾을 수 없습니다.");
        }
        return member;
    }

    private void validateNickname(String nickname) {
        if (!MemberValidationRules.isValidNickname(nickname)) {
            throw new MemberProfileUpdateException("nickname", "닉네임 형식이 올바르지 않습니다.");
        }
    }

    private void validateEmail(String email) {
        if (!MemberValidationRules.isValidEmail(email)) {
            throw new MemberProfileUpdateException("email", "이메일 형식이 올바르지 않습니다.");
        }
    }

    private MemberMyPageResponse toResponse(MemberMyPageInfo member) {
        return new MemberMyPageResponse(
                display(member.loginId()),
                display(member.nickname()),
                display(member.name()),
                display(member.email()),
                maskPhone(member.phone()),
                displayGender(member.gender()),
                member.birthDate() == null ? EMPTY_VALUE : member.birthDate().format(DATE_FORMATTER),
                member.createdAt() == null ? EMPTY_VALUE : member.createdAt().format(DATE_FORMATTER),
                blankToNull(member.profileImageUrl()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return EMPTY_VALUE;
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.length() != 10 && digits.length() != 11) {
            return EMPTY_VALUE;
        }

        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    private String displayGender(String gender) {
        if (gender == null) {
            return EMPTY_VALUE;
        }

        return switch (gender) {
            case "FEMALE" -> "여성";
            case "MALE" -> "남성";
            default -> EMPTY_VALUE;
        };
    }

    private String display(String value) {
        return value == null || value.isBlank() ? EMPTY_VALUE : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
