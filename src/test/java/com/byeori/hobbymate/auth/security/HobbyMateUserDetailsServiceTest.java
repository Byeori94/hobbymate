package com.byeori.hobbymate.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberAuthInfo;

@ExtendWith(MockitoExtension.class)
class HobbyMateUserDetailsServiceTest {

    @Mock
    private MemberDao memberDao;

    @Test
    void activeUserReceivesUserRole() {
        when(memberDao.findAuthByLoginId("member1"))
                .thenReturn(member("member1", "USER", "ACTIVE"));

        UserDetails user = service().loadUserByUsername(" member1 ");

        assertThat(user.getUsername()).isEqualTo("member1");
        assertThat(user).isInstanceOf(HobbyMateUserDetails.class);
        assertThat(((HobbyMateUserDetails) user).getNickname()).isEqualTo("취미회원");
        assertThat(((HobbyMateUserDetails) user).getProfileImageUrl())
                .isEqualTo("/member/profile/1/image");
        assertThat(user.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void administratorReceivesUserAndAdminRoles() {
        when(memberDao.findAuthByLoginId("admin1"))
                .thenReturn(member("admin1", "ADMIN", "ACTIVE"));

        UserDetails user = service().loadUserByUsername("admin1");

        assertThat(user.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void missingAndInactiveMembersAreRejectedWithSameExceptionType() {
        when(memberDao.findAuthByLoginId("missing1")).thenReturn(null);
        when(memberDao.findAuthByLoginId("withdrawn1"))
                .thenReturn(member("withdrawn1", "USER", "WITHDRAWN"));
        when(memberDao.findAuthByLoginId("suspended1"))
                .thenReturn(member("suspended1", "USER", "SUSPENDED"));

        assertThatThrownBy(() -> service().loadUserByUsername("missing1"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service().loadUserByUsername("withdrawn1"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service().loadUserByUsername("suspended1"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void uppercaseOrMalformedLoginIdIsRejectedBeforeDatabaseLookup() {
        assertThatThrownBy(() -> service().loadUserByUsername("Member1"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(memberDao, never()).findAuthByLoginId("Member1");
    }

    private HobbyMateUserDetailsService service() {
        return new HobbyMateUserDetailsService(memberDao);
    }

    private MemberAuthInfo member(String loginId, String role, String status) {
        return new MemberAuthInfo(
                1L,
                loginId,
                "$2a$10$hash",
                "취미회원",
                "/member/profile/1/image",
                role,
                status);
    }
}
