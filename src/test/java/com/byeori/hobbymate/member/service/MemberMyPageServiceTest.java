package com.byeori.hobbymate.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;

@ExtendWith(MockitoExtension.class)
class MemberMyPageServiceTest {

    @Mock
    private MemberDao memberDao;

    @InjectMocks
    private MemberMyPageService memberMyPageService;

    @Test
    void returnsOnlyFormattedMyPageInformation() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(new MemberMyPageInfo(
                "byeori94",
                "벼리",
                "최벼리",
                "sample@example.com",
                "01012341234",
                "FEMALE",
                LocalDate.of(1994, 3, 10),
                LocalDateTime.of(2026, 7, 19, 15, 30),
                null));

        MemberMyPageResponse result = memberMyPageService.getMyPage(7L).orElseThrow();

        assertThat(result.loginId()).isEqualTo("byeori94");
        assertThat(result.nickname()).isEqualTo("벼리");
        assertThat(result.maskedPhone()).isEqualTo("010-****-1234");
        assertThat(result.gender()).isEqualTo("여성");
        assertThat(result.birthDate()).isEqualTo("1994.03.10");
        assertThat(result.joinedDate()).isEqualTo("2026.07.19");
        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    void safelyDisplaysMissingAndUnexpectedValues() {
        when(memberDao.findActiveMemberForMyPage(8L)).thenReturn(new MemberMyPageInfo(
                "member8", null, "", null, "invalid", "UNKNOWN", null, null, " "));

        MemberMyPageResponse result = memberMyPageService.getMyPage(8L).orElseThrow();

        assertThat(result.nickname()).isEqualTo("-");
        assertThat(result.name()).isEqualTo("-");
        assertThat(result.email()).isEqualTo("-");
        assertThat(result.maskedPhone()).isEqualTo("-");
        assertThat(result.gender()).isEqualTo("-");
        assertThat(result.birthDate()).isEqualTo("-");
        assertThat(result.joinedDate()).isEqualTo("-");
        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    void returnsEmptyWhenActiveMemberCannotBeFound() {
        assertThat(memberMyPageService.getMyPage(null)).isEmpty();
        assertThat(memberMyPageService.getMyPage(99L)).isEmpty();
    }
}
