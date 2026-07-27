package com.byeori.hobbymate.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import com.byeori.hobbymate.club.dto.ClubDetailView;
import com.byeori.hobbymate.club.service.ClubDetailService;
import com.byeori.hobbymate.club.vo.ClubDetail;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
class ClubDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubDetailService clubDetailService;

    @BeforeEach
    void setUp() {
        when(clubDetailService.getDetail(eq("1"), isNull()))
                .thenReturn(detailView(
                        "ANONYMOUS", "", "로그인", "로그인 후 가입할 수 있습니다."));
        when(clubDetailService.getDetail("1", 7L))
                .thenReturn(detailView(
                        "LEADER", "모임장", "모임 정보 수정", "내가 운영 중인 모임입니다."));
        when(clubDetailService.getDetail("1", 8L))
                .thenReturn(detailView(
                        "MEMBER", "가입 회원", "모임 활동 보기", "가입한 모임입니다."));
        when(clubDetailService.getDetail("1", 9L))
                .thenReturn(detailView(
                        "MANAGER", "운영진", "모임 관리", "운영진으로 활동 중인 모임입니다."));
    }

    @Test
    void anonymousUserCanViewPublicClubDetail() throws Exception {
        mockMvc.perform(get("/clubs/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("club/detail"))
                .andExpect(content().string(Matchers.containsString("주말 독서 모임")))
                .andExpect(content().string(Matchers.containsString("승인 후 가입")))
                .andExpect(content().string(Matchers.containsString("로그인 후 가입할 수 있습니다.")))
                .andExpect(content().string(Matchers.containsString("aria-current=\"page\"")))
                .andExpect(content().string(Matchers.containsString("공지사항")))
                .andExpect(content().string(Matchers.containsString("자유게시판")))
                .andExpect(content().string(Matchers.containsString("만남모집")))
                .andExpect(content().string(Matchers.containsString("만남후기")))
                .andExpect(content().string(Matchers.containsString("data-club-nav-unavailable")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("모임관리"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("th:utext"))));
    }

    @Test
    void clubNavigationKeepsFixedOrderAndUsesButtonsForUnavailableMenus() throws Exception {
        String html = mockMvc.perform(get("/clubs/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int navigationStart = html.indexOf("aria-label=\"모임 메뉴\"");
        assertThat(navigationStart).isGreaterThanOrEqualTo(0);
        String navigation = html.substring(navigationStart, html.indexOf("</nav>", navigationStart));

        assertThat(navigation.indexOf("홈"))
                .isLessThan(navigation.indexOf("공지사항"));
        assertThat(navigation.indexOf("공지사항"))
                .isLessThan(navigation.indexOf("자유게시판"));
        assertThat(navigation.indexOf("자유게시판"))
                .isLessThan(navigation.indexOf("만남모집"));
        assertThat(navigation.indexOf("만남모집"))
                .isLessThan(navigation.indexOf("만남후기"));
        assertThat(navigation).contains("type=\"button\"", "준비 중인 기능입니다.");
    }

    @Test
    void loggedInLeaderSeesServerDerivedRole() throws Exception {
        mockMvc.perform(get("/clubs/1").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("내가 운영 중인 모임입니다.")))
                .andExpect(content().string(Matchers.containsString("모임 정보 수정")))
                .andExpect(content().string(Matchers.containsString("모임관리")));
    }

    @Test
    void regularMemberDoesNotSeeClubManagementMenu() throws Exception {
        mockMvc.perform(get("/clubs/1").with(user(userDetails(8L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("가입한 모임입니다.")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("모임관리"))));
    }

    @Test
    void activeManagerSeesClubManagementMenu() throws Exception {
        String html = mockMvc.perform(get("/clubs/1").with(user(userDetails(9L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("운영진으로 활동 중인 모임입니다.")))
                .andExpect(content().string(Matchers.containsString("club-internal-menu-manage")))
                .andExpect(content().string(Matchers.containsString("모임관리")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html.indexOf("만남후기")).isLessThan(html.indexOf("모임관리"));
    }

    @Test
    void unavailableAndMalformedClubIdsReturnSafeNotFoundPage() throws Exception {
        when(clubDetailService.getDetail(eq("999"), isNull()))
                .thenThrow(new ClubNotFoundException());
        when(clubDetailService.getDetail(eq("not-number"), isNull()))
                .thenThrow(new ClubNotFoundException());

        mockMvc.perform(get("/clubs/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("club/not-found"))
                .andExpect(content().string(Matchers.containsString(
                        "존재하지 않거나 이용할 수 없는 모임입니다.")));

        mockMvc.perform(get("/clubs/not-number"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("club/not-found"));
    }

    @Test
    void contextPathIsAppliedToDetailResourcesAndLinks() throws Exception {
        mockMvc.perform(get("/hobbymate/clubs/1").contextPath("/hobbymate"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/club-detail.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/common/club-nav.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/common/club-nav.js\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/club-detail.js\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/clubs\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/clubs/1\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/images/logo/hobbymate_logo_transparent.png\"")));
    }

    private ClubDetailView detailView(
            String relationshipType,
            String relationshipLabel,
            String actionLabel,
            String actionMessage) {
        ClubDetail club = new ClubDetail(
                1L,
                null,
                "주말 독서 모임",
                "독서",
                "서울 마포구",
                "함께 책을 읽어요.",
                "매주 한 권을 정해 편안하게 이야기하는 모임입니다.",
                "MIXED",
                20,
                60,
                3,
                20,
                "OPEN",
                "APPROVAL",
                "읽고 싶은 책을 알려주세요.",
                7L,
                "벼리",
                null,
                LocalDateTime.of(2026, 7, 20, 10, 0),
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 26, 12, 0));
        return new ClubDetailView(
                club,
                relationshipType,
                relationshipLabel,
                actionLabel,
                actionMessage);
    }

    private HobbyMateUserDetails userDetails() {
        return userDetails(7L);
    }

    private HobbyMateUserDetails userDetails(Long memberId) {
        return new HobbyMateUserDetails(
                memberId,
                "byeori94",
                "encoded-password",
                "벼리",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
