package com.byeori.hobbymate.admin;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.admin.dto.AdminPage;
import com.byeori.hobbymate.admin.dto.VerificationListView;
import com.byeori.hobbymate.admin.dto.VerificationSearchCondition;
import com.byeori.hobbymate.admin.service.AdminMemberVerificationService;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationState;
import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMemberVerificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMemberVerificationService service;

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mockMvc.perform(get("/admin/member-verifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void regularUserIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/member-verifications").with(user(userDetails(false))))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanOpenPageAndSeesAdminMenu() throws Exception {
        VerificationSearchCondition search = VerificationSearchCondition.of("ALL", "");
        when(service.getList(search, 1, 1)).thenReturn(new VerificationListView(
                AdminPage.of(List.of(), 0, 1),
                AdminPage.of(List.of(), 0, 1),
                search));

        mockMvc.perform(get("/admin/member-verifications").with(user(userDetails(true))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("본인인증처리(임시)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "임시 본인인증 처리 대상 회원이 없습니다.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "임시 본인인증 취소 대상 회원이 없습니다.")));
    }

    @Test
    void mutationRequestWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/admin/member-verifications/7/temporary")
                        .with(user(userDetails(true)))
                        .param("name", "홍길동")
                        .param("birthDate", "1990-01-01")
                        .param("gender", "MALE")
                        .param("phone", "01012345678")
                        .param("reason", "테스트"))
                .andExpect(status().isForbidden());
    }

    @Test
    void processingFormRendersBirthDateInHtmlDateFormat() throws Exception {
        when(service.getProcessingTarget(2L)).thenReturn(new AdminMemberVerificationState(
                2L,
                "byeori94",
                "버리군듀S2",
                "최벼리",
                LocalDate.of(1994, 3, 10),
                "FEMALE",
                "01027582632",
                "USER",
                "ACTIVE",
                "N",
                null,
                null,
                null,
                null));

        mockMvc.perform(get("/admin/member-verifications/2/temporary")
                        .with(user(userDetails(true))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "name=\"birthDate\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "value=\"1994-03-10\"")));
    }

    private HobbyMateUserDetails userDetails(boolean admin) {
        List<SimpleGrantedAuthority> roles = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new HobbyMateUserDetails(
                admin ? 1L : 2L,
                admin ? "admin1" : "member1",
                "password",
                admin ? "관리자" : "회원",
                null,
                roles);
    }
}
