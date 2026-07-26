package com.byeori.hobbymate.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.byeori.hobbymate.admin.dao.AdminMemberVerificationDao;
import com.byeori.hobbymate.admin.dto.BatchVerificationCancelRequest;
import com.byeori.hobbymate.admin.dto.BatchVerificationRequest;
import com.byeori.hobbymate.admin.dto.TemporaryVerificationRequest;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationState;
import com.byeori.hobbymate.admin.vo.AdminVerificationHistory;
import com.byeori.hobbymate.common.exception.AdminVerificationException;

@ExtendWith(MockitoExtension.class)
class AdminMemberVerificationServiceTest {

    @Mock
    private AdminMemberVerificationDao dao;

    private AdminMemberVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AdminMemberVerificationService(dao);
        lenient().when(dao.isAdminMember(1L)).thenReturn(true);
    }

    @Test
    void individualProcessingCreatesServerSideTemporaryCiAndHistory() {
        AdminMemberVerificationState target = pending(7L, "user07");
        when(dao.findMemberStateForUpdate(7L)).thenReturn(target);
        when(dao.updateTemporaryVerification(
                eq(7L), eq("홍길동"), eq(LocalDate.of(1990, 1, 2)),
                eq("MALE"), eq("01012345678"), any(), eq("테스트 인증"), eq(1L)))
                .thenReturn(1);
        when(dao.insertVerificationHistory(any())).thenReturn(1);

        TemporaryVerificationRequest request = new TemporaryVerificationRequest();
        request.setName(" 홍길동 ");
        request.setBirthDate(LocalDate.of(1990, 1, 2));
        request.setGender("MALE");
        request.setPhone("010-1234-5678");
        request.setReason(" 테스트 인증 ");

        assertThat(service.processTemporary(1L, 7L, request)).isEqualTo(1);

        ArgumentCaptor<String> ciCaptor = ArgumentCaptor.forClass(String.class);
        verify(dao).updateTemporaryVerification(
                eq(7L), eq("홍길동"), eq(LocalDate.of(1990, 1, 2)),
                eq("MALE"), eq("01012345678"), ciCaptor.capture(), eq("테스트 인증"), eq(1L));
        assertThat(ciCaptor.getValue()).startsWith("TEMP-7-");

        ArgumentCaptor<AdminVerificationHistory> historyCaptor =
                ArgumentCaptor.forClass(AdminVerificationHistory.class);
        verify(dao).insertVerificationHistory(historyCaptor.capture());
        assertThat(historyCaptor.getValue().actionType()).isEqualTo("ADMIN_TEMP_AUTH");
        assertThat(historyCaptor.getValue().afterMethod()).isEqualTo("ADMIN_TEMP");
        assertThat(historyCaptor.getValue().afterCiHash()).isEqualTo(ciCaptor.getValue());
        assertThat(historyCaptor.getValue().processingType()).isEqualTo("INDIVIDUAL");
    }

    @Test
    void batchValidationFailureOccursBeforeAnyUpdate() {
        BatchVerificationRequest request = new BatchVerificationRequest();
        request.setMemberIds(List.of(7L, 8L));
        request.setReason("일괄 테스트");
        when(dao.findMemberStateForUpdate(7L)).thenReturn(pending(7L, "user07"));
        when(dao.findMemberStateForUpdate(8L)).thenReturn(new AdminMemberVerificationState(
                8L, "user08", "닉네임", "홍길동", null, "MALE", "01012345678",
                "USER", "ACTIVE", "N", null, null, null, null));

        assertThatThrownBy(() -> service.processTemporaryBatch(1L, request))
                .isInstanceOf(AdminVerificationException.class)
                .hasMessageContaining("user08 회원의 생년월일");

        verify(dao, never()).updateTemporaryVerification(
                any(), any(), any(), any(), any(), any(), any(), any());
        verify(dao, never()).insertVerificationHistory(any());
    }

    @Test
    void actualApiVerificationCanNeverBeCanceled() {
        BatchVerificationCancelRequest request = new BatchVerificationCancelRequest();
        request.setMemberIds(List.of(9L));
        request.setReason("취소 테스트");
        when(dao.findMemberStateForUpdate(9L)).thenReturn(new AdminMemberVerificationState(
                9L, "user09", "닉네임", "홍길동", LocalDate.of(1990, 1, 2),
                "MALE", "01012345678", "USER", "ACTIVE", "Y", "PASS",
                null, null, "real-ci-hash"));

        assertThatThrownBy(() -> service.cancelTemporaryBatch(1L, request))
                .isInstanceOf(AdminVerificationException.class)
                .hasMessage("실제 본인인증이 완료된 회원은 관리자 화면에서 취소할 수 없습니다.");

        verify(dao, never()).cancelTemporaryVerification(any(), any());
        verify(dao, never()).insertVerificationHistory(any());
    }

    @Test
    void conditionalUpdateMissIsNotTreatedAsSuccess() {
        when(dao.findMemberStateForUpdate(7L)).thenReturn(pending(7L, "user07"));
        when(dao.updateTemporaryVerification(
                eq(7L), any(), any(), any(), any(), any(), any(), eq(1L)))
                .thenReturn(0);

        TemporaryVerificationRequest request = new TemporaryVerificationRequest();
        request.setName("홍길동");
        request.setBirthDate(LocalDate.of(1990, 1, 2));
        request.setGender("MALE");
        request.setPhone("01012345678");
        request.setReason("테스트");

        assertThatThrownBy(() -> service.processTemporary(1L, 7L, request))
                .isInstanceOf(AdminVerificationException.class)
                .hasMessageContaining("목록을 새로고침");

        verify(dao, never()).insertVerificationHistory(any());
    }

    @Test
    void nonAdministratorCannotMutateMember() {
        when(dao.isAdminMember(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.processTemporaryBatch(2L, new BatchVerificationRequest()))
                .isInstanceOf(AdminVerificationException.class)
                .hasMessage("관리자 권한이 필요합니다.");

        verify(dao, never()).findMemberStateForUpdate(any());
    }

    private AdminMemberVerificationState pending(Long memberId, String loginId) {
        return new AdminMemberVerificationState(
                memberId,
                loginId,
                "닉네임",
                "홍길동",
                LocalDate.of(1990, 1, 2),
                "MALE",
                "01012345678",
                "USER",
                "ACTIVE",
                "N",
                null,
                null,
                null,
                null);
    }
}
