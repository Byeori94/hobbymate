package com.byeori.hobbymate.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;

@SpringBootTest
@AutoConfigureMockMvc
class MemberWithdrawalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MemberDao memberDao;

    @BeforeEach
    void setUpMember() {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(new MemberMyPageInfo(
                "byeori94", "벼리", "최벼리", "sample@example.com", "01012341234",
                "FEMALE", LocalDate.of(1994, 3, 10),
                LocalDateTime.of(2026, 7, 19, 10, 20), null));
    }

    @Test
    void anonymousGetAndPostAreBlockedWithoutUpdate() throws Exception {
        mockMvc.perform(get("/member/mypage/withdraw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        mockMvc.perform(post("/member/mypage/withdraw")
                        .with(csrf())
                        .param("currentPassword", "Current1!")
                        .param("withdrawAgreed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verify(memberDao, never()).withdrawActiveMember(1L);
    }

    @Test
    void formContainsOnlyPasswordAndConsentWithoutMemberIdentifiers() throws Exception {
        mockMvc.perform(get("/member/mypage/withdraw").with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("회원 탈퇴")))
                .andExpect(content().string(Matchers.containsString("name=\"currentPassword\"")))
                .andExpect(content().string(Matchers.containsString("type=\"password\"")))
                .andExpect(content().string(Matchers.containsString("name=\"withdrawAgreed\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"memberId\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"loginId\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("CI_HASH"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("stored-hash"))));

        mockMvc.perform(get("/member/mypage/withdraw")
                        .with(user(userDetails("ROLE_USER", "ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/member/mypage/withdraw")
                        .with(user(userDetails("ROLE_USER")))
                        .param("currentPassword", "Current1!")
                        .param("withdrawAgreed", "true"))
                .andExpect(status().isForbidden());

        verify(memberDao, never()).withdrawActiveMember(1L);
    }

    @Test
    void validationFailureDoesNotRenderSubmittedPassword() throws Exception {
        mockMvc.perform(post("/member/mypage/withdraw")
                        .with(user(userDetails("ROLE_USER")))
                        .with(csrf())
                        .param("currentPassword", "SecretCurrent1!"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "회원 탈퇴 안내를 확인하고 동의해 주세요.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("SecretCurrent1!"))));

        verify(memberDao, never()).findActivePasswordHashByMemberId(1L);
        verify(memberDao, never()).withdrawActiveMember(1L);
    }

    @Test
    void successfulWithdrawalUsesPrincipalThenClearsAuthenticationAndSession() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_USER", "ROLE_ADMIN");
        when(memberDao.findActivePasswordHashByMemberId(1L))
                .thenReturn(passwordEncoder.encode("Current1!"));
        when(memberDao.withdrawActiveMember(1L)).thenReturn(1);

        mockMvc.perform(post("/member/mypage/withdraw")
                        .session(session)
                        .with(csrf())
                        .param("currentPassword", "Current1!")
                        .param("withdrawAgreed", "true")
                        .param("memberId", "999")
                        .param("loginId", "another")
                        .param("memberStatus", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("successMessage", "회원 탈퇴가 완료되었습니다."));

        verify(memberDao).withdrawActiveMember(1L);
        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void contextPathIsAppliedToFormResourcesAndCancelLink() throws Exception {
        mockMvc.perform(get("/hobbymate/member/mypage/withdraw")
                        .contextPath("/hobbymate")
                        .with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/member/mypage/withdraw\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/member/mypage\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/mypage-withdraw.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/mypage-withdraw.js\"")));
    }

    private HobbyMateUserDetails userDetails(String... roles) {
        return new HobbyMateUserDetails(
                1L, "byeori94", "session-hash", "벼리", null,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    private MockHttpSession authenticatedSession(String... roles) {
        HobbyMateUserDetails principal = userDetails(roles);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
