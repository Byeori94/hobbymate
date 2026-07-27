package com.byeori.hobbymate.clubboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.byeori.hobbymate.clubboard.dao.ClubNoticeDao;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@ExtendWith(MockitoExtension.class)
class ClubNoticeServiceTest {

    @Mock
    private ClubNoticeDao clubNoticeDao;

    private ClubNoticeService service;

    @BeforeEach
    void setUp() {
        service = new ClubNoticeService(clubNoticeDao);
    }

    @Test
    void unavailableClubUsesSafeNotFoundException() {
        when(clubNoticeDao.findClubBoardAccess(99L, 7L)).thenReturn(null);

        assertThatThrownBy(() ->
                service.getNotices("99", 7L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubNotFoundException.class)
                .hasMessage("존재하지 않거나 이용할 수 없는 모임입니다.");
        assertThatThrownBy(() ->
                service.getNotices("not-number", 7L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    void onlyActiveClubMembersCanReadNotices() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "LEFT"));

        assertThatThrownBy(() ->
                service.getNotices("1", 7L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubBoardAccessDeniedException.class)
                .hasMessage("모임에 가입한 회원만 공지사항을 확인할 수 있습니다.");
        verify(clubNoticeDao, never()).countNotices(any());
    }

    @Test
    void activeMemberGetsItemsWithDisplayNumbers() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        when(clubNoticeDao.countNotices(any())).thenReturn(35L);
        when(clubNoticeDao.findNotices(any())).thenReturn(List.of(
                notice(10L, "Y"),
                notice(9L, "N")));

        ClubNoticeListView view =
                service.getNotices("1", 7L, new ClubNoticeListRequest());

        assertThat(view.notices().content()).extracting(ClubNoticeListItem::displayNumber)
                .containsExactly(35L, 34L);
        assertThat(view.notices().content().get(0).isPinned()).isTrue();
        assertThat(view.canWriteNotice()).isFalse();
        assertThat(view.canManageClub()).isFalse();
    }

    @Test
    void leaderAndManagerReceiveServerDerivedWritePermission() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "LEADER", "ACTIVE"));
        when(clubNoticeDao.countNotices(any())).thenReturn(0L);

        ClubNoticeListView leader =
                service.getNotices("1", 7L, new ClubNoticeListRequest());
        assertThat(leader.canWriteNotice()).isTrue();
        assertThat(leader.canManageClub()).isTrue();

        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));
        ClubNoticeListView manager =
                service.getNotices("1", 7L, new ClubNoticeListRequest());
        assertThat(manager.canWriteNotice()).isTrue();
        assertThat(manager.canManageClub()).isTrue();
    }

    @Test
    void manipulatedSearchAndPagingValuesFallBackSafely() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        when(clubNoticeDao.countNotices(any())).thenReturn(0L);
        ClubNoticeListRequest request = new ClubNoticeListRequest();
        request.setSearchType("TITLE; DROP TABLE");
        request.setKeyword("a".repeat(101));
        request.setPage("-10");
        request.setPageSize("999");

        ClubNoticeListView view = service.getNotices("1", 7L, request);

        assertThat(view.search().searchType()).isEqualTo("TITLE_CONTENT");
        assertThat(view.search().keyword()).isEmpty();
        assertThat(view.search().page()).isEqualTo(1);
        assertThat(view.search().pageSize()).isEqualTo(20);
        assertThat(view.validationMessages()).containsExactly(
                "검색어는 100자 이하로 입력해 주세요.");
        verify(clubNoticeDao, never()).findNotices(any());
    }

    @Test
    void searchEscapesLikeWildcardsAndOversizedPageIsClamped() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        when(clubNoticeDao.countNotices(any())).thenReturn(21L);
        when(clubNoticeDao.findNotices(any())).thenReturn(List.of(notice(1L, "N")));
        ClubNoticeListRequest request = new ClubNoticeListRequest();
        request.setSearchType("WRITER");
        request.setKeyword("  벼리%_!  ");
        request.setPage("999999");
        request.setPageSize("20");

        ClubNoticeListView view = service.getNotices("1", 7L, request);

        ArgumentCaptor<ClubNoticeSearchCondition> captor =
                ArgumentCaptor.forClass(ClubNoticeSearchCondition.class);
        verify(clubNoticeDao).findNotices(captor.capture());
        ClubNoticeSearchCondition search = captor.getValue();
        assertThat(search.keyword()).isEqualTo("벼리%_!");
        assertThat(search.keywordPattern()).isEqualTo("%벼리!%!_!!%");
        assertThat(search.page()).isEqualTo(2);
        assertThat(search.offset()).isEqualTo(20);
        assertThat(view.notices().content().get(0).displayNumber()).isEqualTo(1L);
    }

    private ClubNoticeListItem notice(Long id, String pinnedYn) {
        return new ClubNoticeListItem(
                id,
                "운영 안내",
                "벼리",
                "LEADER",
                pinnedYn,
                10L,
                LocalDateTime.of(2026, 7, 27, 10, 0),
                LocalDateTime.of(2026, 7, 27, 10, 0),
                null);
    }
}
