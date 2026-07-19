package com.byeori.hobbymate.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.byeori.hobbymate.common.exception.MemberWithdrawalException;
import com.byeori.hobbymate.file.storage.ProfileImageStorage;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberWithdrawalRequest;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalServiceTest {

    @Mock
    private MemberDao memberDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfileImageStorage profileImageStorage;

    private MemberMyPageService service;

    @BeforeEach
    void setUp() {
        service = new MemberMyPageService(memberDao, passwordEncoder, profileImageStorage);
    }

    @Test
    void withdrawsAuthenticatedActiveMemberAfterPasswordVerification() {
        MemberWithdrawalRequest request = request("Current1!", true);
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Current1!", "stored-hash")).thenReturn(true);
        when(memberDao.withdrawActiveMember(7L)).thenReturn(1);

        service.withdrawMember(7L, request);

        verify(passwordEncoder).matches("Current1!", "stored-hash");
        verify(memberDao).withdrawActiveMember(7L);
    }

    @Test
    void consentIsRequiredBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service.withdrawMember(7L, request("Current1!", false)))
                .isInstanceOf(MemberWithdrawalException.class)
                .hasMessage("회원 탈퇴 안내를 확인하고 동의해 주세요.");

        verify(memberDao, never()).findActivePasswordHashByMemberId(7L);
        verify(memberDao, never()).withdrawActiveMember(7L);
    }

    @Test
    void missingOrInactiveMemberCannotWithdraw() {
        assertThatThrownBy(() -> service.withdrawMember(99L, request("Current1!", true)))
                .isInstanceOf(MemberWithdrawalException.class)
                .hasMessage("회원정보를 찾을 수 없습니다.");

        verify(memberDao, never()).withdrawActiveMember(99L);
    }

    @Test
    void wrongCurrentPasswordNeverUpdatesMember() {
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Wrong1!", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.withdrawMember(7L, request("Wrong1!", true)))
                .isInstanceOf(MemberWithdrawalException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.");

        verify(memberDao, never()).withdrawActiveMember(7L);
    }

    @Test
    void zeroUpdatedRowsIsNotTreatedAsSuccess() {
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Current1!", "stored-hash")).thenReturn(true);
        when(memberDao.withdrawActiveMember(7L)).thenReturn(0);

        assertThatThrownBy(() -> service.withdrawMember(7L, request("Current1!", true)))
                .isInstanceOf(MemberWithdrawalException.class)
                .hasMessage("회원 탈퇴를 처리할 수 없습니다.");
    }

    private MemberWithdrawalRequest request(String password, boolean agreed) {
        MemberWithdrawalRequest request = new MemberWithdrawalRequest();
        request.setCurrentPassword(password);
        request.setWithdrawAgreed(agreed);
        return request;
    }
}
