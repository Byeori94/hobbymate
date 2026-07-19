package com.byeori.hobbymate.member.service;

import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.common.exception.MemberJoinException;
import com.byeori.hobbymate.common.validator.MemberValidationRules;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberJoinRequest;
import com.byeori.hobbymate.member.vo.MemberRegistration;

@Service
public class MemberJoinService {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{4,19}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^010\\d{8}$");

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    public MemberJoinService(MemberDao memberDao, PasswordEncoder passwordEncoder) {
        this.memberDao = memberDao;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isLoginIdAvailable(String loginId) {
        String normalized = normalizeLoginId(loginId);
        validateLoginId(normalized);
        return !memberDao.existsByLoginId(normalized);
    }

    public boolean isNicknameAvailable(String nickname) {
        String normalized = MemberValidationRules.normalizeNickname(nickname);
        if (!MemberValidationRules.isValidNickname(normalized)) {
            throw new MemberJoinException("nickname", "닉네임 형식을 확인해 주세요.");
        }
        return !memberDao.existsByNickname(normalized);
    }

    public boolean isEmailAvailable(String email) {
        String normalized = MemberValidationRules.normalizeEmail(email);
        if (!MemberValidationRules.isValidEmail(normalized)) {
            throw new MemberJoinException("email", "이메일 형식을 확인해 주세요.");
        }
        return !memberDao.existsByEmail(normalized);
    }

    @Transactional
    public void join(MemberJoinRequest request) {
        normalize(request);
        validateJoinRequest(request);

        if (memberDao.existsByLoginId(request.getLoginId())) {
            throw new MemberJoinException("loginId", "이미 사용 중인 아이디입니다.");
        }
        if (memberDao.existsByNickname(request.getNickname())) {
            throw new MemberJoinException("nickname", "이미 사용 중인 닉네임입니다.");
        }
        if (memberDao.existsByEmail(request.getEmail())) {
            throw new MemberJoinException("email", "이미 사용 중인 이메일입니다.");
        }

        MemberRegistration member = new MemberRegistration(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getNickname(),
                request.getEmail(),
                request.getPhone(),
                request.getBirthDate(),
                request.getGender());

        if (memberDao.insertMember(member) != 1) {
            throw new MemberJoinException(null, "회원가입 처리 중 오류가 발생했습니다.");
        }
    }

    private void normalize(MemberJoinRequest request) {
        request.setLoginId(normalizeLoginId(request.getLoginId()));
        request.setName(trim(request.getName()));
        request.setNickname(MemberValidationRules.normalizeNickname(request.getNickname()));
        request.setEmail(MemberValidationRules.normalizeEmail(request.getEmail()));
        request.setPhone(normalizePhone(request.getPhone()));
    }

    private void validateJoinRequest(MemberJoinRequest request) {
        validateLoginId(request.getLoginId());
        validatePassword(request.getPassword());
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new MemberJoinException("passwordConfirm", "비밀번호가 일치하지 않습니다.");
        }
        if (request.getName() == null || request.getName().isBlank() || request.getName().length() > 50) {
            throw new MemberJoinException("name", "이름 형식을 확인해 주세요.");
        }
        if (!MemberValidationRules.isValidNickname(request.getNickname())) {
            throw new MemberJoinException("nickname", "닉네임 형식을 확인해 주세요.");
        }
        if (!MemberValidationRules.isValidEmail(request.getEmail())) {
            throw new MemberJoinException("email", "이메일 형식을 확인해 주세요.");
        }
        if (request.getPhone() == null || !PHONE_PATTERN.matcher(request.getPhone()).matches()) {
            throw new MemberJoinException("phone", "휴대폰 번호를 정확히 입력해 주세요.");
        }
        if (request.getBirthDate() == null || !request.getBirthDate().isBefore(LocalDate.now())) {
            throw new MemberJoinException("birthDate", "생년월일을 정확히 입력해 주세요.");
        }
        if (!"FEMALE".equals(request.getGender()) && !"MALE".equals(request.getGender())) {
            throw new MemberJoinException("gender", "성별을 정확히 선택해 주세요.");
        }
        if (!request.isTermsAgreed()) {
            throw new MemberJoinException("termsAgreed", "이용약관에 동의해 주세요.");
        }
        if (!request.isPrivacyAgreed()) {
            throw new MemberJoinException("privacyAgreed", "개인정보 수집 및 이용에 동의해 주세요.");
        }
    }

    private void validateLoginId(String loginId) {
        if (loginId != null && loginId.chars().anyMatch(Character::isUpperCase)) {
            throw new MemberJoinException("loginId", "아이디에는 영문 대문자를 사용할 수 없습니다.");
        }
        if (loginId == null || !LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new MemberJoinException("loginId", "아이디 형식을 확인해 주세요.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 255 || password.chars().anyMatch(Character::isWhitespace)) {
            throw new MemberJoinException("password", "비밀번호 형식을 확인해 주세요.");
        }
        int categories = 0;
        if (password.matches(".*[A-Za-z].*")) categories++;
        if (password.matches(".*\\d.*")) categories++;
        if (password.matches(".*[^A-Za-z0-9\\s].*")) categories++;
        if (categories < 2) {
            throw new MemberJoinException("password", "영문, 숫자, 특수문자 중 2종 이상을 사용해 주세요.");
        }
    }

    private String normalizeLoginId(String value) {
        return trim(value);
    }

    private String normalizePhone(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.replaceAll("[\\s-]", "");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
