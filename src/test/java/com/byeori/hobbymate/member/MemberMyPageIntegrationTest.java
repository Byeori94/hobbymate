package com.byeori.hobbymate.member;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;

@SpringBootTest
@AutoConfigureMockMvc
class MemberMyPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberDao memberDao;

    @BeforeEach
    void setUpMember() {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(new MemberMyPageInfo(
                "byeori94",
                "벼리",
                "최벼리",
                "sample@example.com",
                "01012341234",
                "FEMALE",
                LocalDate.of(1994, 3, 10),
                LocalDateTime.of(2026, 7, 19, 10, 20),
                null));
    }

    @Test
    void anonymousMemberIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/member/mypage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void userSeesOwnFormattedInformationWithoutSensitiveFields() throws Exception {
        mockMvc.perform(get("/member/mypage").with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("벼리님")))
                .andExpect(content().string(Matchers.containsString("@byeori94")))
                .andExpect(content().string(Matchers.containsString("최벼리")))
                .andExpect(content().string(Matchers.containsString("sample@example.com")))
                .andExpect(content().string(Matchers.containsString("010-****-1234")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("01012341234"))))
                .andExpect(content().string(Matchers.containsString("여성")))
                .andExpect(content().string(Matchers.containsString("1994.03.10")))
                .andExpect(content().string(Matchers.containsString("2026.07.19")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/images/common/default-profile.png\"")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("CI_HASH"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("name=\"password\""))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("session-old-hash"))));
    }

    @Test
    void administratorCanOpenOwnMyPage() throws Exception {
        mockMvc.perform(get("/member/mypage").with(user(userDetails("ROLE_USER", "ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void missingActiveMemberClearsAuthenticationAndRedirectsSafely() throws Exception {
        HobbyMateUserDetails missingMember = new HobbyMateUserDetails(
                99L, "missing", "encoded", "없는회원", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/member/mypage").with(user(missingMember)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void contextPathIsAppliedToLinksAndStaticResources() throws Exception {
        mockMvc.perform(get("/hobbymate/member/mypage")
                        .contextPath("/hobbymate")
                        .with(user(userDetails("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/member/mypage\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/mypage.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/images/common/default-profile.png\"")))
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/auth/logout\"")));
    }

    private HobbyMateUserDetails userDetails(String... roles) {
        return new HobbyMateUserDetails(
                1L,
                "byeori94",
                "encoded",
                "벼리",
                null,
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }
}
