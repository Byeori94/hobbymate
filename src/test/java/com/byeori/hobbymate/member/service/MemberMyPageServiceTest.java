package com.byeori.hobbymate.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.byeori.hobbymate.common.exception.MemberProfileUpdateException;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateRequest;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateResult;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;
import com.byeori.hobbymate.member.vo.MemberProfileUpdate;

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

    @Test
    void updatesNicknameAndEmailWithNormalizedValues() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "old@example.com"));
        when(memberDao.updateActiveMemberProfile(any())).thenReturn(1);
        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest(
                "  초록별  ", "  NEW@EXAMPLE.COM  ");

        MemberProfileUpdateResult result = memberMyPageService.updateProfile(7L, request);

        ArgumentCaptor<MemberProfileUpdate> captor = ArgumentCaptor.forClass(MemberProfileUpdate.class);
        verify(memberDao).updateActiveMemberProfile(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new MemberProfileUpdate(7L, "초록별", "new@example.com"));
        assertThat(result).isEqualTo(new MemberProfileUpdateResult("초록별", true));
        assertThat(request.getNickname()).isEqualTo("초록별");
        assertThat(request.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updatesOnlyEmailWithoutNicknameDuplicateLookup() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "old@example.com"));
        when(memberDao.updateActiveMemberProfile(any())).thenReturn(1);

        memberMyPageService.updateProfile(
                7L, new MemberProfileUpdateRequest("벼리", "new@example.com"));

        verify(memberDao, never()).existsNicknameExceptMember(any(), any());
        verify(memberDao).updateActiveMemberProfile(
                new MemberProfileUpdate(7L, "벼리", "new@example.com"));
    }

    @Test
    void updatesNicknameOnlyAfterFinalDuplicateCheck() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "same@example.com"));
        when(memberDao.existsNicknameExceptMember("초록별", 7L)).thenReturn(false);
        when(memberDao.updateActiveMemberProfile(any())).thenReturn(1);

        memberMyPageService.updateProfile(
                7L, new MemberProfileUpdateRequest("초록별", "same@example.com"));

        verify(memberDao).existsNicknameExceptMember("초록별", 7L);
        verify(memberDao).updateActiveMemberProfile(
                new MemberProfileUpdate(7L, "초록별", "same@example.com"));
    }

    @Test
    void skipsUpdateWhenNothingChanged() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "same@example.com"));

        MemberProfileUpdateResult result = memberMyPageService.updateProfile(
                7L, new MemberProfileUpdateRequest("벼리", "same@example.com"));

        assertThat(result.changed()).isFalse();
        verify(memberDao, never()).existsNicknameExceptMember(any(), any());
        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void duplicateNicknamePreventsUpdate() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "same@example.com"));
        when(memberDao.existsNicknameExceptMember("중복닉", 7L)).thenReturn(true);

        assertThatThrownBy(() -> memberMyPageService.updateProfile(
                7L, new MemberProfileUpdateRequest("중복닉", "same@example.com")))
                .isInstanceOf(MemberProfileUpdateException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");

        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void currentNicknameIsAvailableWithoutDuplicateLookup() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "same@example.com"));

        assertThat(memberMyPageService.isNicknameAvailable(7L, " 벼리 ")).isTrue();
        verify(memberDao, never()).existsNicknameExceptMember(any(), any());
    }

    @Test
    void missingOrWithdrawnMemberCannotBeUpdated() {
        assertThatThrownBy(() -> memberMyPageService.updateProfile(
                99L, new MemberProfileUpdateRequest("닉네임", "mail@example.com")))
                .isInstanceOf(MemberProfileUpdateException.class)
                .hasMessage("회원정보를 찾을 수 없습니다.");

        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void zeroUpdatedRowsIsNotTreatedAsSuccess() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "old@example.com"));
        when(memberDao.updateActiveMemberProfile(any())).thenReturn(0);

        assertThatThrownBy(() -> memberMyPageService.updateProfile(
                7L, new MemberProfileUpdateRequest("벼리", "new@example.com")))
                .isInstanceOf(MemberProfileUpdateException.class)
                .hasMessage("회원정보를 수정할 수 없습니다.");
    }

    @Test
    void concurrentNicknameConstraintViolationUsesSafeFieldError() {
        when(memberDao.findActiveMemberForMyPage(7L)).thenReturn(member("벼리", "old@example.com"));
        when(memberDao.existsNicknameExceptMember("초록별", 7L)).thenReturn(false, true);
        when(memberDao.updateActiveMemberProfile(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate constraint"));

        assertThatThrownBy(() -> memberMyPageService.updateProfile(
                7L, new MemberProfileUpdateRequest("초록별", "old@example.com")))
                .isInstanceOf(MemberProfileUpdateException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");
    }

    private MemberMyPageInfo member(String nickname, String email) {
        return new MemberMyPageInfo(
                "byeori94",
                nickname,
                "최벼리",
                email,
                "01012341234",
                "FEMALE",
                LocalDate.of(1994, 3, 10),
                LocalDateTime.of(2026, 7, 19, 15, 30),
                null);
    }
}
