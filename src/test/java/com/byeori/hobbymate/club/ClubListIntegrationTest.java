package com.byeori.hobbymate.club;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.club.dto.ClubListView;
import com.byeori.hobbymate.club.dto.ClubPage;
import com.byeori.hobbymate.club.dto.ClubSearchCondition;
import com.byeori.hobbymate.club.service.ClubListService;
import com.byeori.hobbymate.club.vo.ClubCategory;
import com.byeori.hobbymate.club.vo.ClubListItem;

@SpringBootTest
@AutoConfigureMockMvc
class ClubListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubListService clubListService;

    @BeforeEach
    void setUp() {
        when(clubListService.getList(any())).thenReturn(viewData());
    }

    @Test
    void anonymousUserCanViewPublicClubList() throws Exception {
        mockMvc.perform(get("/clubs"))
                .andExpect(status().isOk())
                .andExpect(view().name("club/list"))
                .andExpect(content().string(Matchers.containsString("모임 둘러보기")))
                .andExpect(content().string(Matchers.containsString("주말 독서 모임")))
                .andExpect(content().string(Matchers.containsString("3")))
                .andExpect(content().string(Matchers.containsString("20")))
                .andExpect(content().string(Matchers.containsString("href=\"/clubs/1\"")))
                .andExpect(content().string(Matchers.containsString("모임 상세 보기")));
    }

    @Test
    void contextPathIsAppliedToSearchResourcesAndImages() throws Exception {
        mockMvc.perform(get("/hobbymate/clubs").contextPath("/hobbymate"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/clubs\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/css/pages/club-list.css\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/club-list.js\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/images/logo/hobbymate_logo_transparent.png\"")))
                .andExpect(content().string(Matchers.containsString(
                        "href=\"/hobbymate/clubs/1\"")));
    }

    @Test
    void unsafeClubImageRequestIsPublicButReturnsNotFound() throws Exception {
        mockMvc.perform(get("/club-images/not-a-safe-name.png"))
                .andExpect(status().isNotFound());
    }

    private ClubListView viewData() {
        ClubSearchCondition search = new ClubSearchCondition(
                "ALL", "", "", null, "", "", "ALL", null,
                "ALL", "RECENT_ACTIVITY", 1, 20);
        ClubListItem club = new ClubListItem(
                1L,
                null,
                "주말 독서 모임",
                "독서",
                "서울 마포구",
                "함께 책을 읽어요.",
                "MIXED",
                20,
                60,
                3,
                20,
                "OPEN",
                "APPROVAL",
                "벼리",
                LocalDateTime.of(2026, 7, 26, 12, 0),
                LocalDateTime.of(2026, 7, 20, 10, 0));
        return new ClubListView(
                ClubPage.of(List.of(club), 1, 1, 20),
                search,
                List.of(new ClubCategory(3L, "독서")),
                List.of());
    }
}
