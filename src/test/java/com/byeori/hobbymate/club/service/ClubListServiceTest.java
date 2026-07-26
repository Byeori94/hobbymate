package com.byeori.hobbymate.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.dto.ClubListRequest;
import com.byeori.hobbymate.club.dto.ClubListView;
import com.byeori.hobbymate.club.dto.ClubSearchCondition;
import com.byeori.hobbymate.club.vo.ClubCategory;
import com.byeori.hobbymate.club.vo.ClubListItem;

@ExtendWith(MockitoExtension.class)
class ClubListServiceTest {

    @Mock
    private ClubDao clubDao;

    private ClubListService service;

    @BeforeEach
    void setUp() {
        service = new ClubListService(clubDao);
        when(clubDao.findActiveCategories())
                .thenReturn(List.of(new ClubCategory(3L, "독서")));
    }

    @Test
    void defaultSearchUsesRecentActivityAndTwentyItems() {
        when(clubDao.countPublicClubs(any())).thenReturn(1L);
        when(clubDao.findPublicClubs(any())).thenReturn(List.of(sampleClub()));

        ClubListView view = service.getList(new ClubListRequest());

        assertThat(view.search().searchType()).isEqualTo("ALL");
        assertThat(view.search().sortType()).isEqualTo("RECENT_ACTIVITY");
        assertThat(view.search().page()).isEqualTo(1);
        assertThat(view.search().pageSize()).isEqualTo(20);
        assertThat(view.clubs().content()).hasSize(1);
    }

    @Test
    void complexConditionsAreNormalizedAndPassedAsBoundValues() {
        ClubListRequest request = new ClubListRequest();
        request.setSearchType("DESCRIPTION");
        request.setKeyword("  100%_! 취미  ");
        request.setCategoryId("3");
        request.setRegion(" 서울_ ");
        request.setGenderPolicy("MIXED");
        request.setAge("30");
        request.setRecruitStatus("OPEN");
        request.setSortType("MEMBER_COUNT");
        request.setPage("3");
        request.setPageSize("20");
        when(clubDao.countPublicClubs(any())).thenReturn(45L);
        when(clubDao.findPublicClubs(any())).thenReturn(List.of(sampleClub()));

        ClubListView view = service.getList(request);

        ArgumentCaptor<ClubSearchCondition> captor =
                ArgumentCaptor.forClass(ClubSearchCondition.class);
        verify(clubDao).findPublicClubs(captor.capture());
        ClubSearchCondition search = captor.getValue();
        assertThat(search.keyword()).isEqualTo("100%_! 취미");
        assertThat(search.keywordPattern()).isEqualTo("%100!%!_!! 취미%");
        assertThat(search.regionPattern()).isEqualTo("%서울!_%");
        assertThat(search.categoryId()).isEqualTo(3L);
        assertThat(search.age()).isEqualTo(30);
        assertThat(search.offset()).isEqualTo(40);
        assertThat(view.validationMessages()).isEmpty();
    }

    @Test
    void manipulatedCodesAndRangesFallBackSafely() {
        ClubListRequest request = new ClubListRequest();
        request.setSearchType("NAME;DROP TABLE");
        request.setCategoryId("999");
        request.setGenderPolicy("UNKNOWN");
        request.setAge("17");
        request.setRecruitStatus("BLOCKED");
        request.setSortType("CLUB_ID DESC");
        request.setPage("-99");
        request.setPageSize("999");
        when(clubDao.countPublicClubs(any())).thenReturn(0L);

        ClubListView view = service.getList(request);

        assertThat(view.search().searchType()).isEqualTo("ALL");
        assertThat(view.search().categoryId()).isNull();
        assertThat(view.search().genderPolicy()).isEqualTo("ALL");
        assertThat(view.search().age()).isNull();
        assertThat(view.search().recruitStatus()).isEqualTo("ALL");
        assertThat(view.search().sortType()).isEqualTo("RECENT_ACTIVITY");
        assertThat(view.search().page()).isEqualTo(1);
        assertThat(view.search().pageSize()).isEqualTo(20);
        assertThat(view.validationMessages()).hasSize(2);
        verify(clubDao, never()).findPublicClubs(any());
    }

    @Test
    void oversizedPageIsClampedToLastPage() {
        ClubListRequest request = new ClubListRequest();
        request.setPage("999999999");
        request.setPageSize("20");
        when(clubDao.countPublicClubs(any())).thenReturn(21L);
        when(clubDao.findPublicClubs(any())).thenReturn(List.of(sampleClub()));

        ClubListView view = service.getList(request);

        assertThat(view.clubs().page()).isEqualTo(2);
        assertThat(view.search().offset()).isEqualTo(20);
    }

    @Test
    void allowedPageSizesArePreserved() {
        for (String pageSize : List.of("20", "50", "100")) {
            ClubListRequest request = new ClubListRequest();
            request.setPageSize(pageSize);
            when(clubDao.countPublicClubs(any())).thenReturn(0L);

            ClubListView view = service.getList(request);

            assertThat(view.search().pageSize()).isEqualTo(Integer.parseInt(pageSize));
        }
    }

    private ClubListItem sampleClub() {
        return new ClubListItem(
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
    }
}
