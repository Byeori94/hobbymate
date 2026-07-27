package com.byeori.hobbymate.clubboard;

import static org.mockito.ArgumentMatchers.any;
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
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeDetailView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeReturnQuery;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.service.ClubNoticeService;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeAdjacentPost;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeDetail;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;
import com.byeori.hobbymate.common.exception.ClubNoticeCreationException;
import com.byeori.hobbymate.common.exception.ClubNoticeNotFoundException;

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
        when(clubNoticeService.prepareCreation("1", 8L))
                .thenReturn(new ClubNoticeCreateView(1L, "주말 독서 모임", true, true));
        when(clubNoticeService.getNoticeDetail(eq("1"), eq("10"), eq(7L), any()))
                .thenReturn(detailView(false));
        when(clubNoticeService.getNoticeDetail(eq("1"), eq("10"), eq(8L), any()))
                .thenReturn(detailView(true));
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/clubs/1/notices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
    }

    @Test
    void anonymousUserIsRedirectedBeforeNoticeDetailServiceRuns() throws Exception {
        mockMvc.perform(get("/clubs/1/notices/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));
        verify(clubNoticeService, never())
                .getNoticeDetail(any(), any(), any(), any());
    }

    @Test
    void activeMemberCanViewNoticeListWithNoticeMenuActive() throws Exception {
        mockMvc.perform(get("/clubs/1/notices").with(user(userDetails(7L))))
                .andExpect(status().isOk())
                .andExpect(view().name("clubboard/notice-list"))
                .andExpect(content().string(Matchers.containsString("공지사항")))
                .andExpect(content().string(Matchers.containsString("고정")))
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
    void activeMemberCanReadEscapedNoticeDetailAndReturnToSearchedPage() throws Exception {
        mockMvc.perform(get("/clubs/1/notices/10")
                        .param("page", "2")
                        .param("pageSize", "20")
                        .param("searchType", "TITLE")
                        .param("keyword", "운영")
                        .with(user(userDetails(7L))))
                .andExpect(status().isOk())
                .andExpect(view().name("clubboard/notice-detail"))
                .andExpect(content().string(Matchers.containsString("공지 상세 제목")))
                .andExpect(content().string(Matchers.containsString("첫째 줄")))
                .andExpect(content().string(Matchers.containsString(
                        "&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("<script>alert('x')</script>"))))
                .andExpect(content().string(Matchers.containsString("조회")))
                .andExpect(content().string(Matchers.containsString(">13<")))
                .andExpect(content().string(Matchers.containsString(
                        "page=2&amp;pageSize=20&amp;searchType=TITLE")))
                .andExpect(content().string(Matchers.containsString(
                        "keyword=%EC%9A%B4%EC%98%81")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/clubs/1/notices/9?page=2")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/clubs/1/notices/11?page=2")))
                .andExpect(content().string(Matchers.containsString(
                        "aria-current=\"page\"")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("공지사항 수정 기능은 준비 중입니다."))))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("공지사항 삭제 기능은 준비 중입니다."))));
    }

    @Test
    void leaderSeesNonSubmittingEditAndDeleteButtonsWithExactMessages() throws Exception {
        mockMvc.perform(get("/clubs/1/notices/10").with(user(userDetails(8L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "data-message=\"공지사항 수정 기능은 준비 중입니다.\"")))
                .andExpect(content().string(Matchers.containsString(
                        "data-message=\"공지사항 삭제 기능은 준비 중입니다.\"")))
                .andExpect(content().string(Matchers.containsString(
                        "type=\"button\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/js/pages/club-notice-detail.js\"")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("/notices/10/edit"))));
    }

    @Test
    void leaderSeesActualWriteLinkAndManagementMenu() throws Exception {
        mockMvc.perform(get("/clubs/1/notices").with(user(userDetails(8L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("공지 작성")))
                .andExpect(content().string(Matchers.containsString("모임관리")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/clubs/1/notices/new\"")));
    }

    @Test
    void leaderCanOpenCreationFormWithNoticeMenuActive() throws Exception {
        mockMvc.perform(get("/clubs/1/notices/new").with(user(userDetails(8L))))
                .andExpect(status().isOk())
                .andExpect(view().name("clubboard/notice-form"))
                .andExpect(content().string(Matchers.containsString("공지사항 작성")))
                .andExpect(content().string(Matchers.containsString("상단 고정")))
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/clubs/1/notices\"")))
                .andExpect(content().string(Matchers.containsString(
                        "aria-current=\"page\"")));
    }

    @Test
    void ordinaryMemberCannotOpenCreationForm() throws Exception {
        when(clubNoticeService.prepareCreation("1", 7L))
                .thenThrow(ClubNoticeCreationException.accessDenied(1L));

        mockMvc.perform(get("/clubs/1/notices/new").with(user(userDetails(7L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clubs/1"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "모임장과 운영진만 공지사항을 작성할 수 있습니다."));
    }

    @Test
    void postRequiresCsrf() throws Exception {
        mockMvc.perform(post("/clubs/1/notices")
                        .param("title", "정상 공지 제목")
                        .param("content", "열 자 이상인 정상 공지 내용입니다.")
                        .with(user(userDetails(8L))))
                .andExpect(status().isForbidden());
        verify(clubNoticeService, never()).createNotice(any(), any(), any());
    }

    @Test
    void validPostRedirectsWithFlashMessage() throws Exception {
        when(clubNoticeService.createNotice(eq("1"), eq(8L), any()))
                .thenReturn(101L);

        mockMvc.perform(post("/clubs/1/notices")
                        .param("title", "정상 공지 제목")
                        .param("content", "열 자 이상인 정상 공지 내용입니다.")
                        .param("pinnedYn", "Y")
                        .with(user(userDetails(8L)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clubs/1/notices"))
                .andExpect(flash().attribute(
                        "successMessage", "공지사항이 등록되었습니다."));
        verify(clubNoticeService).createNotice(eq("1"), eq(8L), any());
    }

    @Test
    void validationErrorPreservesFormAndDoesNotCreatePost() throws Exception {
        mockMvc.perform(post("/clubs/1/notices")
                        .param("title", " ")
                        .param("content", "짧음")
                        .param("pinnedYn", "Y")
                        .with(user(userDetails(8L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("clubboard/notice-form"))
                .andExpect(content().string(Matchers.containsString("제목을 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString(
                        "내용은 10자 이상 10,000자 이하로 입력해 주세요.")))
                .andExpect(content().string(Matchers.containsString(
                        "id=\"pinnedYn\"")))
                .andExpect(content().string(Matchers.containsString("checked")));
        verify(clubNoticeService, never()).createNotice(any(), any(), any());
    }

    @Test
    void manipulatedPinnedValueIsRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/clubs/1/notices")
                        .param("title", "정상 공지 제목")
                        .param("content", "열 자 이상인 정상 공지 내용입니다.")
                        .param("pinnedYn", "DROP")
                        .with(user(userDetails(8L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "상단 고정 값이 올바르지 않습니다.")));
        verify(clubNoticeService, never()).createNotice(any(), any(), any());
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
    void unavailableNoticeReturnsSafe404AndNoInternalInformation() throws Exception {
        when(clubNoticeService.getNoticeDetail(eq("1"), eq("999"), eq(7L), any()))
                .thenThrow(new ClubNoticeNotFoundException());

        mockMvc.perform(get("/clubs/1/notices/999").with(user(userDetails(7L))))
                .andExpect(status().isNotFound())
                .andExpect(view().name("clubboard/notice-not-found"))
                .andExpect(content().string(Matchers.containsString(
                        "존재하지 않거나 확인할 수 없는 공지사항입니다.")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("HM_CLUB_POST"))));
    }

    @Test
    void inactiveMemberDetailAccessRedirectsToClubHome() throws Exception {
        when(clubNoticeService.getNoticeDetail(eq("2"), eq("10"), eq(7L), any()))
                .thenThrow(new ClubBoardAccessDeniedException(2L));

        mockMvc.perform(get("/clubs/2/notices/10").with(user(userDetails(7L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clubs/2"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "모임에 가입한 회원만 공지사항을 확인할 수 있습니다."));
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

    @Test
    void listTitlesLinkToActualDetailAndPreserveCurrentCondition() throws Exception {
        mockMvc.perform(get("/clubs/1/notices")
                        .param("searchType", "TITLE_CONTENT")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .with(user(userDetails(7L))))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/clubs/1/notices/10?page=1&amp;pageSize=20"
                                + "&amp;searchType=TITLE_CONTENT&amp;keyword=\"")))
                .andExpect(content().string(Matchers.not(
                        Matchers.containsString("상세 조회는 준비 중입니다."))));
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

    private ClubNoticeDetailView detailView(boolean canManage) {
        ClubNoticeDetail detail = new ClubNoticeDetail(
                10L,
                1L,
                8L,
                "공지 상세 제목",
                "첫째 줄\n둘째 줄 <script>alert('x')</script>",
                "벼리",
                "LEADER",
                "Y",
                13L,
                LocalDateTime.of(2026, 7, 27, 10, 0),
                LocalDateTime.of(2026, 7, 27, 11, 0));
        return new ClubNoticeDetailView(
                1L,
                "주말 독서 모임",
                canManage,
                canManage,
                canManage,
                detail,
                new ClubNoticeAdjacentPost(
                        9L,
                        "이전 공지",
                        LocalDateTime.of(2026, 7, 26, 10, 0)),
                new ClubNoticeAdjacentPost(
                        11L,
                        "다음 공지",
                        LocalDateTime.of(2026, 7, 28, 10, 0)),
                new ClubNoticeReturnQuery(2, 20, "TITLE", "운영"));
    }
}
