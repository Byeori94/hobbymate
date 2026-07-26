package com.byeori.hobbymate.club;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.club.dto.ClubCreationPage;
import com.byeori.hobbymate.club.service.ClubCreationService;
import com.byeori.hobbymate.club.vo.ClubCategory;

@SpringBootTest
@AutoConfigureMockMvc
class ClubCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubCreationService clubCreationService;

    @BeforeEach
    void setUp() {
        when(clubCreationService.prepareCreation(1L))
                .thenReturn(new ClubCreationPage(List.of(new ClubCategory(3L, "독서"))));
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/clubs/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void eligibleUserCanOpenCreationForm() throws Exception {
        mockMvc.perform(get("/clubs/new").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(view().name("club/create"))
                .andExpect(content().string(Matchers.containsString("모임 개설")))
                .andExpect(content().string(Matchers.containsString("독서")))
                .andExpect(content().string(Matchers.containsString("action=\"/clubs\"")));
    }

    @Test
    void postRequiresCsrfToken() throws Exception {
        mockMvc.perform(validRequest().with(user(userDetails())))
                .andExpect(status().isForbidden());
    }

    @Test
    void validFormCreatesClubAndRedirectsWithMessage() throws Exception {
        when(clubCreationService.createClub(eq(1L), any(), any())).thenReturn(51L);

        mockMvc.perform(validRequest().with(user(userDetails())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("successMessage", "모임이 개설되었습니다."));

        verify(clubCreationService).createClub(eq(1L), any(), any());
    }

    @Test
    void invalidFormReturnsFieldErrorsWithoutCallingCreate() throws Exception {
        MockMultipartFile emptyImage =
                new MockMultipartFile("representativeImage", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/clubs")
                        .file(emptyImage)
                        .param("clubName", " ")
                        .param("categoryId", "3")
                        .param("activityRegion", "서울")
                        .param("clubSummary", "소개")
                        .param("clubDescription", "짧음")
                        .param("genderPolicy", "MIXED")
                        .param("minAge", "50")
                        .param("maxAge", "20")
                        .param("maxMemberCount", "1")
                        .param("joinType", "APPROVAL")
                        .with(user(userDetails()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("club/create"))
                .andExpect(content().string(Matchers.containsString("모임명을 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString(
                        "최대 정원은 2명 이상이어야 합니다.")));
    }

    @Test
    void contextPathIsAppliedToFormAndResources() throws Exception {
        mockMvc.perform(get("/hobbymate/clubs/new")
                        .contextPath("/hobbymate")
                        .with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/clubs\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/club-create.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/club-create.js\"")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            validRequest() {
        MockMultipartFile emptyImage =
                new MockMultipartFile("representativeImage", "", "application/octet-stream", new byte[0]);
        return multipart("/clubs")
                .file(emptyImage)
                .param("clubName", "주말 독서 모임")
                .param("categoryId", "3")
                .param("activityRegion", "서울 마포구")
                .param("clubSummary", "책을 함께 읽어요")
                .param("clubDescription", "매주 한 권을 정해 편안하게 이야기하는 모임입니다.")
                .param("genderPolicy", "MIXED")
                .param("minAge", "20")
                .param("maxAge", "60")
                .param("maxMemberCount", "20")
                .param("joinType", "APPROVAL")
                .param("joinGuide", "읽고 싶은 책을 알려주세요.");
    }

    private HobbyMateUserDetails userDetails() {
        return new HobbyMateUserDetails(
                1L,
                "byeori94",
                "encoded-password",
                "벼리",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
