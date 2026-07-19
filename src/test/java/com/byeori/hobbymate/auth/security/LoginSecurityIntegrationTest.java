package com.byeori.hobbymate.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberAuthInfo;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class LoginSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MemberDao memberDao;

    @BeforeEach
    void setUpMembers() {
        when(memberDao.findAuthByLoginId("member1"))
                .thenReturn(member("member1", "correct-password", "USER", "ACTIVE"));
        when(memberDao.findAuthByLoginId("admin1"))
                .thenReturn(member("admin1", "correct-password", "ADMIN", "ACTIVE"));
        when(memberDao.findAuthByLoginId("withdrawn1"))
                .thenReturn(member("withdrawn1", "correct-password", "USER", "WITHDRAWN"));
        when(memberDao.findAuthByLoginId("profile1"))
                .thenReturn(member(
                        "profile1",
                        "correct-password",
                        "USER",
                        "ACTIVE",
                        "/member/profile/1/image"));
    }

    @Test
    void loginPageAndStaticResourcesArePublic() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("영문 소문자와 숫자만 입력 가능합니다.")));

        mockMvc.perform(get("/css/pages/login.css"))
                .andExpect(status().isOk());
    }

    @Test
    void activeMemberLogsInAndReceivesUserRole() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", " member1 ")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("member1").withRoles("USER"));
    }

    @Test
    void administratorReceivesBothRoles() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", "admin1")
                        .param("password", "correct-password"))
                .andExpect(authenticated().withUsername("admin1").withRoles("USER", "ADMIN"));
    }

    @Test
    void authenticatedHeaderDisplaysNicknameInsteadOfLoginId() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", "member1")
                        .param("password", "correct-password"))
                .andReturn();

        mockMvc.perform(get("/")
                        .session((org.springframework.mock.web.MockHttpSession)
                                login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "취미회원</span>님")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "src=\"/images/common/default-profile.png\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "aria-expanded=\"false\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/member/mypage\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "action=\"/auth/logout\" method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(">member1</span>"))));
    }

    @Test
    void anonymousHeaderDisplaysLoginAndJoinLinks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/auth/login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/auth/join\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("class=\"profile-menu\""))));
    }

    @Test
    void authenticatedHeaderUsesStoredProfileImageUrlWhenPresent() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", "profile1")
                        .param("password", "correct-password"))
                .andReturn();

        mockMvc.perform(get("/")
                        .session((org.springframework.mock.web.MockHttpSession)
                                login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "src=\"/member/profile/1/image\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(
                                "src=\"/images/common/default-profile.png\""))));
    }

    @Test
    void missingWrongPasswordAndWithdrawnMemberUseSameFailureRedirect() throws Exception {
        assertLoginFails("missing1", "correct-password");
        assertLoginFails("member1", "wrong-password");
        assertLoginFails("withdrawn1", "correct-password");

        mockMvc.perform(get("/auth/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @Test
    void savedRequestIsUsedAfterSuccessfulLogin() throws Exception {
        MvcResult protectedRequest = mockMvc.perform(get("/member/mypage"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .session((org.springframework.mock.web.MockHttpSession)
                                protectedRequest.getRequest().getSession(false))
                        .with(csrf())
                        .param("loginId", "member1")
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(login.getResponse().getRedirectedUrl()).contains("/member/mypage");
    }

    @Test
    void savedIdCookieChangesOnlyAfterSuccessfulLogin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", "member1")
                        .param("password", "correct-password")
                        .param("saveId", "true"))
                .andExpect(cookie().value(SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME, "member1"))
                .andExpect(cookie().maxAge(SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME,
                        60 * 60 * 24 * 30));

        Cookie existingCookie = new Cookie(
                SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME, "member1");
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .cookie(existingCookie)
                        .param("loginId", "member2")
                        .param("password", "wrong-password"))
                .andExpect(cookie().doesNotExist(
                        SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME));
    }

    @Test
    void savedCookiePrefillsLoginPage() throws Exception {
        Cookie savedId = new Cookie(SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME, "member1");
        mockMvc.perform(get("/auth/login").cookie(savedId))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"member1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("checked=\"checked\"")));
    }

    @Test
    void postLogoutInvalidatesAuthenticatedSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", "member1")
                        .param("password", "correct-password"))
                .andReturn();

        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(authenticated().withUsername("member1"));

        mockMvc.perform(get("/auth/logout").session(session))
                .andExpect(status().isNotFound())
                .andExpect(authenticated().withUsername("member1"));

        Cookie savedId = new Cookie(SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME, "member1");
        mockMvc.perform(post("/auth/logout")
                        .session(session)
                        .cookie(savedId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?logout"))
                .andExpect(unauthenticated())
                .andExpect(cookie().doesNotExist(
                        SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME));
    }

    private void assertLoginFails(String loginId, String password) throws Exception {
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("loginId", loginId)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated());
    }

    private MemberAuthInfo member(String loginId, String rawPassword, String role, String status) {
        return member(loginId, rawPassword, role, status, null);
    }

    private MemberAuthInfo member(
            String loginId,
            String rawPassword,
            String role,
            String status,
            String profileImageUrl) {
        return new MemberAuthInfo(
                1L,
                loginId,
                passwordEncoder.encode(rawPassword),
                "취미회원",
                profileImageUrl,
                role,
                status);
    }
}
