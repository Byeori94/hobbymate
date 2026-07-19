package com.byeori.hobbymate.member.service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;

@Service
public class MemberMyPageService {

    private static final String EMPTY_VALUE = "-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final MemberDao memberDao;

    public MemberMyPageService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Transactional(readOnly = true)
    public Optional<MemberMyPageResponse> getMyPage(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(memberDao.findActiveMemberForMyPage(memberId))
                .map(this::toResponse);
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
