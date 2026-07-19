package com.byeori.hobbymate.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
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
class MemberPasswordChangeIntegrationTest {

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
    void anonymousGetAndPostAreRedirectedWithoutUpdating() throws Exception {
        mockMvc.perform(get("/member/mypage/password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        mockMvc.perform(post("/member/mypage/password")
                        .with(csrf())
                        .param("currentPassword", "Current1!")
                        .param("newPassword", "Changed2@")
                        .param("newPasswordConfirm", "Changed2@"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verify(memberDao, never()).updateActiveMemberPassword(eq(1L), anyString());
    }

    @Test
    void formUsesSafePasswordFieldsAndContainsNoMemberIdentifier() throws Exception {
        mockMvc.perform(get("/member/mypage/password").with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("비밀번호 변경")))
                .andExpect(content().string(Matchers.containsString("name=\"currentPassword\"")))
                .andExpect(content().string(Matchers.containsString("autocomplete=\"current-password\"")))
                .andExpect(content().string(Matchers.containsString("name=\"newPassword\"")))
                .andExpect(content().string(Matchers.containsString("autocomplete=\"new-password\"")))
                .andExpect(content().string(Matchers.containsString("type=\"password\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"memberId\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"loginId\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("stored-hash"))));
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/member/mypage/password")
                        .with(user(userDetails("ROLE_USER")))
                        .param("currentPassword", "Current1!")
                        .param("newPassword", "Changed2@")
                        .param("newPasswordConfirm", "Changed2@"))
                .andExpect(status().isForbidden());

        verify(memberDao, never()).updateActiveMemberPassword(eq(1L), anyString());
    }

    @Test
    void validationFailureDoesNotRenderSubmittedPasswords() throws Exception {
        mockMvc.perform(post("/member/mypage/password")
                        .with(user(userDetails("ROLE_USER")))
                        .with(csrf())
                        .param("currentPassword", "SecretCurrent1!")
                        .param("newPassword", "short")
                        .param("newPasswordConfirm", "DifferentSecret2@"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "새 비밀번호는 8자 이상 255자 이하로 입력해 주세요.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("SecretCurrent1!"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("DifferentSecret2@"))));

        verify(memberDao, never()).findActivePasswordHashByMemberId(1L);
    }

    @Test
    void successfulChangeUsesAuthenticatedMemberAndKeepsAdminSession() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_USER", "ROLE_ADMIN");
        String storedHash = passwordEncoder.encode("Current1!");
        when(memberDao.findActivePasswordHashByMemberId(1L)).thenReturn(storedHash);
        when(memberDao.updateActiveMemberPassword(eq(1L), anyString()))
                .thenReturn(1);

        mockMvc.perform(post("/member/mypage/password")
                        .session(session)
                        .with(csrf())
                        .param("currentPassword", "Current1!")
                        .param("newPassword", "Changed2@")
                        .param("newPasswordConfirm", "Changed2@")
                        .param("memberId", "999")
                        .param("loginId", "another"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(flash().attribute("successMessage", "비밀번호가 변경되었습니다."));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberDao).updateActiveMemberPassword(eq(1L), hashCaptor.capture());
        assertThat(hashCaptor.getValue()).startsWith("$2");
        assertThat(passwordEncoder.matches("Changed2@", hashCaptor.getValue())).isTrue();
        assertThat(passwordEncoder.matches("Current1!", hashCaptor.getValue())).isFalse();

        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().isAuthenticated()).isTrue();
        assertThat(context.getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void contextPathIsAppliedToActionResourcesAndCancelLink() throws Exception {
        mockMvc.perform(get("/hobbymate/member/mypage/password")
                        .contextPath("/hobbymate")
                        .with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/member/mypage/password\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/member/mypage\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/mypage-password.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/mypage-password.js\"")));
    }

    private HobbyMateUserDetails userDetails(String... roles) {
        return new HobbyMateUserDetails(
                1L, "byeori94", "session-old-hash", "벼리", null,
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
