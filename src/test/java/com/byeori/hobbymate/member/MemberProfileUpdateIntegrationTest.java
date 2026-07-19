package com.byeori.hobbymate.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;
import com.byeori.hobbymate.member.vo.MemberProfileUpdate;

@SpringBootTest
@AutoConfigureMockMvc
class MemberProfileUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberDao memberDao;

    @BeforeEach
    void setUpMember() {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member("벼리", "old@example.com"));
    }

    @Test
    void anonymousGetAndPostAreRedirectedWithoutUpdatingDatabase() throws Exception {
        mockMvc.perform(get("/member/mypage/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        mockMvc.perform(post("/member/mypage/edit")
                        .with(csrf())
                        .param("nickname", "초록별")
                        .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void editFormShowsOnlySafeEditableAndReadonlyInformation() throws Exception {
        mockMvc.perform(get("/member/mypage/edit").with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("회원정보 수정")))
                .andExpect(content().string(Matchers.containsString("value=\"벼리\"")))
                .andExpect(content().string(Matchers.containsString("value=\"old@example.com\"")))
                .andExpect(content().string(Matchers.containsString("byeori94")))
                .andExpect(content().string(Matchers.containsString("최벼리")))
                .andExpect(content().string(Matchers.containsString("010-****-1234")))
                .andExpect(content().string(Matchers.containsString("1994.03.10")))
                .andExpect(content().string(Matchers.containsString("type=\"button\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"memberId\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"loginId\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"password\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("CI_HASH"))));
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/member/mypage/edit")
                        .with(user(userDetails("ROLE_USER")))
                        .param("nickname", "초록별")
                        .param("email", "new@example.com"))
                .andExpect(status().isForbidden());

        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void validationErrorsRestoreReadonlyInformationAndSubmittedValues() throws Exception {
        mockMvc.perform(post("/member/mypage/edit")
                        .with(user(userDetails("ROLE_USER")))
                        .with(csrf())
                        .param("nickname", "   ")
                        .param("email", "invalid-email"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("닉네임을 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString("이메일 형식이 올바르지 않습니다.")))
                .andExpect(content().string(Matchers.containsString("value=\"invalid-email\"")))
                .andExpect(content().string(Matchers.containsString("최벼리")))
                .andExpect(content().string(Matchers.containsString("010-****-1234")));

        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void nicknameCheckAllowsCurrentValueAndRejectsAnotherMembersValue() throws Exception {
        mockMvc.perform(get("/member/mypage/check-nickname")
                        .with(user(userDetails("ROLE_USER")))
                        .param("value", "벼리"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"available\":true,\"message\":\"사용 가능한 닉네임입니다.\"}"));

        when(memberDao.existsNicknameExceptMember("중복닉", 1L)).thenReturn(true);
        mockMvc.perform(get("/member/mypage/check-nickname")
                        .with(user(userDetails("ROLE_USER")))
                        .param("value", "중복닉"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"available\":false,\"message\":\"이미 사용 중인 닉네임입니다.\"}"));
    }

    @Test
    void successfulUpdateRefreshesNicknameAndKeepsAdminAuthority() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_USER", "ROLE_ADMIN");
        when(memberDao.existsNicknameExceptMember("초록별", 1L)).thenReturn(false);
        when(memberDao.updateActiveMemberProfile(any())).thenReturn(1);

        mockMvc.perform(post("/member/mypage/edit")
                        .session(session)
                        .with(csrf())
                        .param("nickname", " 초록별 ")
                        .param("email", " NEW@EXAMPLE.COM ")
                        .param("memberId", "999")
                        .param("memberRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(flash().attribute("successMessage", "회원정보가 수정되었습니다."));

        ArgumentCaptor<MemberProfileUpdate> captor = ArgumentCaptor.forClass(MemberProfileUpdate.class);
        verify(memberDao).updateActiveMemberProfile(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new MemberProfileUpdate(1L, "초록별", "new@example.com"));

        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        HobbyMateUserDetails updated = (HobbyMateUserDetails) context.getAuthentication().getPrincipal();
        assertThat(updated.getNickname()).isEqualTo("초록별");
        assertThat(context.getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
        assertThat(context.getAuthentication().getDetails()).isEqualTo("existing-details");

        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member("초록별", "new@example.com"));
        mockMvc.perform(get("/member/mypage").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("초록별</span>님")));
    }

    @Test
    void unchangedSubmissionSkipsUpdateAndUsesOneTimeMessage() throws Exception {
        mockMvc.perform(post("/member/mypage/edit")
                        .with(user(userDetails("ROLE_USER")))
                        .with(csrf())
                        .param("nickname", "벼리")
                        .param("email", "old@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(flash().attribute("successMessage", "변경된 정보가 없습니다."));

        verify(memberDao, never()).updateActiveMemberProfile(any());
    }

    @Test
    void contextPathIsAppliedToFormAjaxResourcesAndCancelLink() throws Exception {
        mockMvc.perform(get("/hobbymate/member/mypage/edit")
                        .contextPath("/hobbymate")
                        .with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/member/mypage/edit\"")))
                .andExpect(content().string(Matchers.containsString(
                        "data-check-nickname-url=\"/hobbymate/member/mypage/check-nickname\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/member/mypage\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/mypage-edit.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/mypage-edit.js\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/images/common/default-profile.png\"")));
    }

    private MockHttpSession authenticatedSession(String... roles) {
        HobbyMateUserDetails principal = userDetails(roles);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities());
        authentication.setDetails("existing-details");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private HobbyMateUserDetails userDetails(String... roles) {
        return new HobbyMateUserDetails(
                1L,
                "byeori94",
                "encoded",
                "벼리",
                null,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
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
                LocalDateTime.of(2026, 7, 19, 10, 20),
                null);
    }
}
