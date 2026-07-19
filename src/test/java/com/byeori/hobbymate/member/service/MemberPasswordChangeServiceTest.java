package com.byeori.hobbymate.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.byeori.hobbymate.common.exception.MemberPasswordChangeException;
import com.byeori.hobbymate.file.storage.ProfileImageStorage;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberPasswordChangeRequest;

@ExtendWith(MockitoExtension.class)
class MemberPasswordChangeServiceTest {

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
    void changesOnlyActiveMembersPasswordAfterAllChecks() {
        MemberPasswordChangeRequest request = request("Current1!", "Changed2@", "Changed2@");
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Current1!", "stored-hash")).thenReturn(true);
        when(passwordEncoder.matches("Changed2@", "stored-hash")).thenReturn(false);
        when(passwordEncoder.encode("Changed2@")).thenReturn("new-hash");
        when(memberDao.updateActiveMemberPassword(7L, "new-hash")).thenReturn(1);

        service.changePassword(7L, request);

        InOrder order = inOrder(memberDao, passwordEncoder);
        order.verify(memberDao).findActivePasswordHashByMemberId(7L);
        order.verify(passwordEncoder).matches("Current1!", "stored-hash");
        order.verify(passwordEncoder).matches("Changed2@", "stored-hash");
        order.verify(passwordEncoder).encode("Changed2@");
        order.verify(memberDao).updateActiveMemberPassword(7L, "new-hash");
    }

    @Test
    void mismatchedConfirmationIsRejectedBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service.changePassword(
                7L, request("Current1!", "Changed2@", "Different3#")))
                .isInstanceOf(MemberPasswordChangeException.class)
                .hasMessage("새 비밀번호와 확인값이 일치하지 않습니다.");

        verify(memberDao, never()).findActivePasswordHashByMemberId(7L);
        verify(memberDao, never()).updateActiveMemberPassword(anyLong(), anyString());
    }

    @Test
    void invalidNewPasswordIncludingWhitespaceIsRejectedWithoutTrimming() {
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Current1!", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(
                7L, request("Current1!", " Changed2@ ", " Changed2@ ")))
                .isInstanceOf(MemberPasswordChangeException.class)
                .hasMessage("새 비밀번호 형식이 올바르지 않습니다.");

        verify(memberDao, never()).updateActiveMemberPassword(anyLong(), anyString());
    }

    @Test
    void wrongCurrentPasswordNeverEncodesOrUpdates() {
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Wrong1!", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(
                7L, request("Wrong1!", "Changed2@", "Changed2@")))
                .isInstanceOf(MemberPasswordChangeException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.");

        verify(passwordEncoder, never()).encode(anyString());
        verify(memberDao, never()).updateActiveMemberPassword(anyLong(), anyString());
    }

    @Test
    void reusingStoredPasswordIsRejectedUsingMatches() {
        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Current1!", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(
                7L, request("Current1!", "Current1!", "Current1!")))
                .isInstanceOf(MemberPasswordChangeException.class)
                .hasMessage("새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.");

        verify(passwordEncoder, never()).encode(anyString());
        verify(memberDao, never()).updateActiveMemberPassword(anyLong(), anyString());
    }

    @Test
    void missingOrInactiveMemberAndZeroRowUpdateAreNotSuccess() {
        assertThatThrownBy(() -> service.changePassword(
                99L, request("Current1!", "Changed2@", "Changed2@")))
                .isInstanceOf(MemberPasswordChangeException.class)
                .hasMessage("회원정보를 찾을 수 없습니다.");

        when(memberDao.findActivePasswordHashByMemberId(7L)).thenReturn("stored-hash");
        when(passwordEncoder.matches("Current1!", "stored-hash")).thenReturn(true);
        when(passwordEncoder.matches("Changed2@", "stored-hash")).thenReturn(false);
        when(passwordEncoder.encode("Changed2@")).thenReturn("new-hash");
        when(memberDao.updateActiveMemberPassword(7L, "new-hash")).thenReturn(0);

        assertThatThrownBy(() -> service.changePassword(
                7L, request("Current1!", "Changed2@", "Changed2@")))
                .isInstanceOf(MemberPasswordChangeException.class)
                .hasMessage("비밀번호를 변경할 수 없습니다.");
    }

    private MemberPasswordChangeRequest request(String current, String password, String confirm) {
        MemberPasswordChangeRequest request = new MemberPasswordChangeRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(password);
        request.setNewPasswordConfirm(confirm);
        return request;
    }

}
