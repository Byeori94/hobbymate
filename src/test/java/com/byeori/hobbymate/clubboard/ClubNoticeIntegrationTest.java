package com.byeori.hobbymate.clubboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import com.byeori.hobbymate.club.dto.ClubPage;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.service.ClubNoticeService;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
class ClubNoticeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubNoticeService clubNoticeService;

    @BeforeEach
    void setUp() {
        when(clubNoticeService.getNotices(eq("1"), eq(7L), any()))
                .thenReturn(viewData("MEMBER", false, false));
        when(clubNoticeService.getNotices(eq("1"), eq(8L), any()))
                .thenReturn(viewData("LEADER", true, true));
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/clubs/1/notices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
    }

    @Test
    void activeMemberCanViewNoticeListWithNoticeMenuActive() throws Exception {
        mockMvc.perform(get("/clubs/1/notices").with(user(userDetails(7L))))
                .andExpect(status().isOk())
                .andExpect(view().name("clubboard/notice-list"))
                .andExpect(content().string(Matchers.containsString("공지사항")))
                .andExpect(content().string(Matchers.containsString("중요")))
                .andExpect(content().string(Matchers.containsString("운영 안내")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/clubs/1/notices\"")))
                .andExpect(content().string(Matchers.containsString(
                        "aria-current=\"page\"")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("공지 작성"))))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("모임관리"))));
    }

    @Test
    void leaderSeesPreparedWriteButtonAndManagementMenu() throws Exception {
        mockMvc.perform(get("/clubs/1/notices").with(user(userDetails(8L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("공지 작성")))
                .andExpect(content().string(Matchers.containsString("준비 중")))
                .andExpect(content().string(Matchers.containsString("모임관리")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("/notices/new"))));
    }

    @Test
    void inaccessibleMembershipRedirectsToClubHomeWithMessage() throws Exception {
        when(clubNoticeService.getNotices(eq("2"), eq(7L), any()))
                .thenThrow(new ClubBoardAccessDeniedException(2L));

        mockMvc.perform(get("/clubs/2/notices").with(user(userDetails(7L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clubs/2"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "모임에 가입한 회원만 공지사항을 확인할 수 있습니다."));
    }

    @Test
    void unavailableClubReturnsSafeNotFoundPage() throws Exception {
        when(clubNoticeService.getNotices(eq("999"), eq(7L), any()))
                .thenThrow(new ClubNotFoundException());

        mockMvc.perform(get("/clubs/999/notices").with(user(userDetails(7L))))
                .andExpect(status().isNotFound())
                .andExpect(view().name("club/not-found"))
                .andExpect(content().string(Matchers.containsString(
                        "존재하지 않거나 이용할 수 없는 모임입니다.")));
    }

    @Test
    void contextPathIsAppliedToResourcesAndLinks() throws Exception {
        mockMvc.perform(get("/hobbymate/clubs/1/notices")
                        .contextPath("/hobbymate")
                        .param("searchType", "TITLE")
                        .param("keyword", "운영")
                        .with(user(userDetails(7L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/clubs/1/notices\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/clubs/1\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/club-notice-list.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/common/club-nav.js\"")));
    }

    @Test
    void pagingLinksPreserveSearchTypeKeywordAndPageSize() throws Exception {
        ClubNoticeListView searchedView = new ClubNoticeListView(
                3L,
                "검색 테스트 모임",
                "MEMBER",
                false,
                false,
                ClubPage.of(
                        List.of(new ClubNoticeListItem(
                                30L, "운영 검색 결과", "벼리", "LEADER", "N", 1L,
                                LocalDateTime.of(2026, 7, 27, 10, 0),
                                LocalDateTime.of(2026, 7, 27, 10, 0), 25L)),
                        45L,
                        2,
                        20),
                new ClubNoticeSearchCondition(
                        3L, "TITLE", "운영", "%운영%", 2, 20),
                List.of());
        when(clubNoticeService.getNotices(eq("3"), eq(7L), any()))
                .thenReturn(searchedView);

        mockMvc.perform(get("/clubs/3/notices")
                        .param("searchType", "TITLE")
                        .param("keyword", "운영")
                        .param("page", "2")
                        .param("pageSize", "20")
                        .with(user(userDetails(7L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "searchType=TITLE")))
                .andExpect(content().string(Matchers.containsString(
                        "keyword=%EC%9A%B4%EC%98%81")))
                .andExpect(content().string(Matchers.containsString(
                        "pageSize=20")));
    }

    private ClubNoticeListView viewData(
            String role,
            boolean canManage,
            boolean canWrite) {
        ClubNoticeListItem pinned = new ClubNoticeListItem(
                10L, "운영 안내", "벼리", "LEADER", "Y", 12L,
                LocalDateTime.of(2026, 7, 27, 10, 0),
                LocalDateTime.of(2026, 7, 27, 11, 0), 2L);
        ClubNoticeListItem normal = new ClubNoticeListItem(
                9L, "이번 주 활동 안내", "운영진", "MANAGER", "N", 4L,
                LocalDateTime.of(2026, 7, 26, 10, 0),
                LocalDateTime.of(2026, 7, 26, 10, 0), 1L);
        ClubNoticeSearchCondition search = new ClubNoticeSearchCondition(
                1L, "TITLE_CONTENT", "", "", 1, 20);
        return new ClubNoticeListView(
                1L,
                "주말 독서 모임",
                role,
                canManage,
                canWrite,
                ClubPage.of(List.of(pinned, normal), 2L, 1, 20),
                search,
                List.of());
    }

    private HobbyMateUserDetails userDetails(Long memberId) {
        return new HobbyMateUserDetails(
                memberId,
                "member" + memberId,
                "encoded-password",
                "벼리",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
