package com.byeori.hobbymate.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.byeori.hobbymate.common.exception.MemberJoinException;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.dto.MemberJoinRequest;
import com.byeori.hobbymate.member.vo.MemberRegistration;

@ExtendWith(MockitoExtension.class)
class MemberJoinServiceTest {

    @Mock
    private MemberDao memberDao;

    private MemberJoinService service;

    @BeforeEach
    void setUp() {
        service = new MemberJoinService(memberDao, new BCryptPasswordEncoder());
    }

    @Test
    void uppercaseLoginIdIsRejectedBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service.isLoginIdAvailable("Byeori94"))
                .isInstanceOf(MemberJoinException.class)
                .hasMessage("아이디에는 영문 대문자를 사용할 수 없습니다.");

        verify(memberDao, never()).existsByLoginId(any());
    }

    @Test
    void malformedLoginIdIsRejectedBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service.isLoginIdAvailable("94byeori"))
                .isInstanceOf(MemberJoinException.class)
                .hasMessage("아이디 형식을 확인해 주세요.");

        verify(memberDao, never()).existsByLoginId(any());
    }

    @Test
    void duplicateLoginIdPreventsInsert() {
        MemberJoinRequest request = validRequest();
        when(memberDao.existsByLoginId("byeori94")).thenReturn(true);

        assertThatThrownBy(() -> service.join(request))
                .isInstanceOf(MemberJoinException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");

        verify(memberDao, never()).insertMember(any());
    }

    @Test
    void passwordMismatchPreventsInsert() {
        MemberJoinRequest request = validRequest();
        request.setPasswordConfirm("Different123!");

        assertThatThrownBy(() -> service.join(request))
                .isInstanceOf(MemberJoinException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");

        verify(memberDao, never()).insertMember(any());
    }

    @Test
    void futureBirthDatePreventsInsert() {
        MemberJoinRequest request = validRequest();
        request.setBirthDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.join(request))
                .isInstanceOf(MemberJoinException.class)
                .hasMessage("생년월일을 정확히 입력해 주세요.");
    }

    @Test
    void validMemberIsNormalizedEncryptedAndInserted() {
        MemberJoinRequest request = validRequest();
        request.setLoginId("  byeori94  ");
        request.setEmail("  BYEORI@EXAMPLE.COM  ");
        request.setPhone("010-1234-5678");
        when(memberDao.insertMember(any())).thenReturn(1);

        service.join(request);

        ArgumentCaptor<MemberRegistration> captor = ArgumentCaptor.forClass(MemberRegistration.class);
        verify(memberDao).insertMember(captor.capture());
        MemberRegistration saved = captor.getValue();
        assertThat(saved.loginId()).isEqualTo("byeori94");
        assertThat(saved.email()).isEqualTo("byeori@example.com");
        assertThat(saved.phone()).isEqualTo("01012345678");
        assertThat(saved.encodedPassword()).startsWith("$2");
        assertThat(saved.encodedPassword()).isNotEqualTo(request.getPassword());
    }

    private MemberJoinRequest validRequest() {
        MemberJoinRequest request = new MemberJoinRequest();
        request.setLoginId("byeori94");
        request.setPassword("Password1!");
        request.setPasswordConfirm("Password1!");
        request.setName("최벼리");
        request.setNickname("벼리");
        request.setEmail("byeori@example.com");
        request.setPhone("01012345678");
        request.setBirthDate(LocalDate.of(1994, 1, 1));
        request.setGender("FEMALE");
        request.setTermsAgreed(true);
        request.setPrivacyAgreed(true);
        return request;
    }
}
