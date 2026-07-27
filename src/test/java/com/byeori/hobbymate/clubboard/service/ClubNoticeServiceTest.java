package com.byeori.hobbymate.clubboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeDetailView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeEditView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeUpdateRequest;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeAdjacentPost;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeDetail;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.clubboard.vo.ClubPostCreation;
import com.byeori.hobbymate.clubboard.vo.ClubPostUpdate;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;
import com.byeori.hobbymate.common.exception.ClubNoticeCreationException;
import com.byeori.hobbymate.common.exception.ClubNoticeDetailException;
import com.byeori.hobbymate.common.exception.ClubNoticeManagementAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNoticeNotFoundException;
import com.byeori.hobbymate.common.exception.ClubNoticeUpdateException;

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
    void activeMemberCanReadDetailAndViewCountIsIncrementedAtomically() {
        ClubNoticeDetail before = detail(10L, 12L);
        ClubNoticeDetail after = detail(10L, 13L);
        ClubNoticeAdjacentPost previous = adjacent(9L, "이전 공지", 9);
        ClubNoticeAdjacentPost next = adjacent(11L, "다음 공지", 11);
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(before, after);
        when(clubNoticeDao.incrementNoticeViewCount(1L, 10L)).thenReturn(1);
        when(clubNoticeDao.findPreviousNotice(1L, 10L, after.createdAt()))
                .thenReturn(previous);
        when(clubNoticeDao.findNextNotice(1L, 10L, after.createdAt()))
                .thenReturn(next);
        ClubNoticeListRequest request = new ClubNoticeListRequest();
        request.setPage("2");
        request.setPageSize("50");
        request.setSearchType("TITLE");
        request.setKeyword(" 운영 ");

        ClubNoticeDetailView view =
                service.getNoticeDetail("1", "10", 7L, request);

        assertThat(view.notice().viewCount()).isEqualTo(13L);
        assertThat(view.previousNotice()).isEqualTo(previous);
        assertThat(view.nextNotice()).isEqualTo(next);
        assertThat(view.canEditNotice()).isFalse();
        assertThat(view.canDeleteNotice()).isFalse();
        assertThat(view.returnQuery().page()).isEqualTo(2);
        assertThat(view.returnQuery().pageSize()).isEqualTo(50);
        assertThat(view.returnQuery().searchType()).isEqualTo("TITLE");
        assertThat(view.returnQuery().keyword()).isEqualTo("운영");
        verify(clubNoticeDao, times(2)).findNoticeDetail(1L, 10L);
        verify(clubNoticeDao).incrementNoticeViewCount(1L, 10L);
    }

    @Test
    void currentActiveLeaderOrManagerReceivesDetailManagementButtons() {
        ClubNoticeDetail detail = detail(10L, 3L);
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(detail, detail);
        when(clubNoticeDao.incrementNoticeViewCount(1L, 10L)).thenReturn(1);

        ClubNoticeDetailView view =
                service.getNoticeDetail("1", "10", 7L, new ClubNoticeListRequest());

        assertThat(view.canManageClub()).isTrue();
        assertThat(view.canEditNotice()).isTrue();
        assertThat(view.canDeleteNotice()).isTrue();
    }

    @Test
    void inaccessibleOrMissingNoticeNeverIncrementsViewCount() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "LEFT"));

        assertThatThrownBy(() ->
                service.getNoticeDetail("1", "10", 7L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubBoardAccessDeniedException.class);
        verify(clubNoticeDao, never()).findNoticeDetail(any(), any());
        verify(clubNoticeDao, never()).incrementNoticeViewCount(any(), any());

        when(clubNoticeDao.findClubBoardAccess(1L, 8L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        when(clubNoticeDao.findNoticeDetail(1L, 99L)).thenReturn(null);
        assertThatThrownBy(() ->
                service.getNoticeDetail("1", "99", 8L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubNoticeNotFoundException.class)
                .hasMessage("존재하지 않거나 확인할 수 없는 공지사항입니다.");
        verify(clubNoticeDao, never()).incrementNoticeViewCount(1L, 99L);
    }

    @Test
    void invalidPostIdIsSafeNotFoundAndFailedIncrementIsNotSuccessful() {
        assertThatThrownBy(() ->
                service.getNoticeDetail("1", "not-number", 7L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubNoticeNotFoundException.class);

        ClubNoticeDetail detail = detail(10L, 12L);
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(detail);
        when(clubNoticeDao.incrementNoticeViewCount(1L, 10L)).thenReturn(0);

        assertThatThrownBy(() ->
                service.getNoticeDetail("1", "10", 7L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubNoticeDetailException.class)
                .hasMessage("공지사항을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        verify(clubNoticeDao, times(1)).findNoticeDetail(1L, 10L);
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

    @Test
    void onlyActiveLeaderOrManagerCanOpenCreationForm() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));

        ClubNoticeCreateView view = service.prepareCreation("1", 7L);

        assertThat(view.clubName()).isEqualTo("주말 독서 모임");
        assertThat(view.canWriteNotice()).isTrue();
        assertThat(view.canManageClub()).isTrue();

        when(clubNoticeDao.findClubBoardAccess(1L, 8L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));
        assertThatThrownBy(() -> service.prepareCreation("1", 8L))
                .isInstanceOf(ClubNoticeCreationException.class)
                .hasMessage("모임장과 운영진만 공지사항을 작성할 수 있습니다.");
    }

    @Test
    void createsNoticeWithServerOwnedValuesAndTrimmedText() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "LEADER", "ACTIVE"));
        when(clubNoticeDao.lockClubForPostMutation(1L)).thenReturn(1L);
        doAnswer(invocation -> {
            ClubPostCreation post = invocation.getArgument(0);
            post.setPostId(101L);
            return 1;
        }).when(clubNoticeDao).insertClubPost(any());
        ClubNoticeCreateRequest request = request(
                "  8월 운영 안내  ",
                "  모임 운영과 일정에 관한 안내 내용입니다.  ",
                null);

        Long postId = service.createNotice("1", 7L, request);

        assertThat(postId).isEqualTo(101L);
        ArgumentCaptor<ClubPostCreation> captor =
                ArgumentCaptor.forClass(ClubPostCreation.class);
        verify(clubNoticeDao).insertClubPost(captor.capture());
        ClubPostCreation post = captor.getValue();
        assertThat(post.getClubId()).isEqualTo(1L);
        assertThat(post.getAuthorMemberId()).isEqualTo(7L);
        assertThat(post.getPostType()).isEqualTo("NOTICE");
        assertThat(post.getTitle()).isEqualTo("8월 운영 안내");
        assertThat(post.getContent()).isEqualTo("모임 운영과 일정에 관한 안내 내용입니다.");
        assertThat(post.getPinnedYn()).isEqualTo("N");
        verify(clubNoticeDao, never()).countPinnedPosts(any(), any());
    }

    @Test
    void pinnedNoticeIsLimitedToFiveWithinSameClubAndBoard() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));
        when(clubNoticeDao.lockClubForPostMutation(1L)).thenReturn(1L);
        when(clubNoticeDao.countPinnedPosts(1L, "NOTICE")).thenReturn(5);

        assertThatThrownBy(() -> service.createNotice(
                "1",
                7L,
                request("상단 고정 안내", "상단에 고정할 충분히 긴 공지 내용입니다.", "Y")))
                .isInstanceOf(ClubNoticeCreationException.class)
                .hasMessage("상단 고정 게시글은 게시판별로 최대 5개까지 등록할 수 있습니다.")
                .extracting("fieldName")
                .isEqualTo("pinnedYn");
        verify(clubNoticeDao, never()).insertClubPost(any());
    }

    @Test
    void serviceRejectsTrimmedLengthAndManipulatedPinnedValue() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "LEADER", "ACTIVE"));
        when(clubNoticeDao.lockClubForPostMutation(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.createNotice(
                "1", 7L, request(" a ", "열 자 이상인 정상 공지 내용입니다.", "N")))
                .isInstanceOf(ClubNoticeCreationException.class)
                .hasMessage("제목은 2자 이상 입력해 주세요.");
        assertThatThrownBy(() -> service.createNotice(
                "1", 7L, request("정상 제목", "열 자 이상인 정상 공지 내용입니다.", "PIN")))
                .isInstanceOf(ClubNoticeCreationException.class)
                .hasMessage("상단 고정 값이 올바르지 않습니다.");
        verify(clubNoticeDao, never()).insertClubPost(any());
    }

    @Test
    void ordinaryMemberCannotLockClubOrInsertByCallingPostDirectly() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));

        assertThatThrownBy(() -> service.createNotice(
                "1",
                7L,
                request("권한 조작 공지", "권한 없는 회원이 작성하려는 충분히 긴 내용입니다.", "Y")))
                .isInstanceOf(ClubNoticeCreationException.class)
                .hasMessage("모임장과 운영진만 공지사항을 작성할 수 있습니다.");
        verify(clubNoticeDao, never()).lockClubForPostMutation(any());
        verify(clubNoticeDao, never()).insertClubPost(any());
        verify(clubNoticeDao, never()).countPinnedPosts(any(), eq("NOTICE"));
    }

    @Test
    void currentManagerCanOpenEditFormWithoutIncreasingViewCount() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(detail(10L, 12L));

        ClubNoticeEditView view = service.prepareUpdate(
                "1", "10", 7L, new ClubNoticeListRequest());

        assertThat(view.title()).isEqualTo("운영 안내");
        assertThat(view.content()).isEqualTo("첫째 줄\n둘째 줄");
        assertThat(view.pinnedYn()).isEqualTo("Y");
        assertThat(view.canManageClub()).isTrue();
        verify(clubNoticeDao, never()).incrementNoticeViewCount(any(), any());
    }

    @Test
    void updateTrimsEditableFieldsAndPreservesServerOwnedColumnsInMapperCommand() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "LEADER", "ACTIVE"));
        when(clubNoticeDao.lockClubForPostMutation(1L)).thenReturn(1L);
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(detail(10L, 12L));
        when(clubNoticeDao.countPinnedPostsExcluding(1L, "NOTICE", 10L)).thenReturn(4);
        when(clubNoticeDao.updateClubPost(any())).thenReturn(1);
        ClubNoticeUpdateRequest request = updateRequest(
                "  수정 공지 제목  ",
                "  수정한 공지 내용은 충분히 깁니다.  ",
                "Y");

        service.updateNotice(
                "1", "10", 7L, request, new ClubNoticeListRequest());

        ArgumentCaptor<ClubPostUpdate> captor =
                ArgumentCaptor.forClass(ClubPostUpdate.class);
        verify(clubNoticeDao).updateClubPost(captor.capture());
        ClubPostUpdate update = captor.getValue();
        assertThat(update.clubId()).isEqualTo(1L);
        assertThat(update.postId()).isEqualTo(10L);
        assertThat(update.title()).isEqualTo("수정 공지 제목");
        assertThat(update.content()).isEqualTo("수정한 공지 내용은 충분히 깁니다.");
        assertThat(update.pinnedYn()).isEqualTo("Y");
    }

    @Test
    void updatePinnedLimitExcludesCurrentNotice() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));
        when(clubNoticeDao.lockClubForPostMutation(1L)).thenReturn(1L);
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(detail(10L, 12L));
        when(clubNoticeDao.countPinnedPostsExcluding(1L, "NOTICE", 10L)).thenReturn(5);

        assertThatThrownBy(() -> service.updateNotice(
                "1",
                "10",
                7L,
                updateRequest(
                        "수정 공지 제목",
                        "수정한 공지 내용은 충분히 깁니다.",
                        "Y"),
                new ClubNoticeListRequest()))
                .isInstanceOf(ClubNoticeUpdateException.class)
                .hasMessage("상단 고정 게시글은 게시판별로 최대 5개까지 설정할 수 있습니다.")
                .extracting("fieldName")
                .isEqualTo("pinnedYn");
        verify(clubNoticeDao, never()).updateClubPost(any());
    }

    @Test
    void ordinaryMemberCannotUpdateOrDeleteEvenWhenOriginalAuthor() {
        when(clubNoticeDao.findClubBoardAccess(1L, 8L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MEMBER", "ACTIVE"));

        assertThatThrownBy(() -> service.updateNotice(
                "1",
                "10",
                8L,
                updateRequest(
                        "수정 공지 제목",
                        "수정한 공지 내용은 충분히 깁니다.",
                        "N"),
                new ClubNoticeListRequest()))
                .isInstanceOf(ClubNoticeManagementAccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteNotice(
                "1", "10", 8L, new ClubNoticeListRequest()))
                .isInstanceOf(ClubNoticeManagementAccessDeniedException.class);
        verify(clubNoticeDao, never()).lockClubForPostMutation(any());
        verify(clubNoticeDao, never()).updateClubPost(any());
        verify(clubNoticeDao, never()).softDeleteClubPost(any(), any());
    }

    @Test
    void managerCanLogicallyDeleteAnActiveNotice() {
        when(clubNoticeDao.findClubBoardAccess(1L, 7L))
                .thenReturn(new ClubBoardAccess(1L, "주말 독서 모임", "MANAGER", "ACTIVE"));
        when(clubNoticeDao.lockClubForPostMutation(1L)).thenReturn(1L);
        when(clubNoticeDao.findNoticeDetail(1L, 10L)).thenReturn(detail(10L, 12L));
        when(clubNoticeDao.softDeleteClubPost(1L, 10L)).thenReturn(1);

        service.deleteNotice("1", "10", 7L, new ClubNoticeListRequest());

        verify(clubNoticeDao).softDeleteClubPost(1L, 10L);
    }

    private ClubNoticeCreateRequest request(
            String title,
            String content,
            String pinnedYn) {
        ClubNoticeCreateRequest request = new ClubNoticeCreateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setPinnedYn(pinnedYn);
        return request;
    }

    private ClubNoticeUpdateRequest updateRequest(
            String title,
            String content,
            String pinnedYn) {
        ClubNoticeUpdateRequest request = new ClubNoticeUpdateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setPinnedYn(pinnedYn);
        return request;
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

    private ClubNoticeDetail detail(Long postId, long viewCount) {
        return new ClubNoticeDetail(
                postId,
                1L,
                8L,
                "운영 안내",
                "첫째 줄\n둘째 줄",
                "벼리",
                "LEADER",
                "Y",
                viewCount,
                LocalDateTime.of(2026, 7, 27, 10, 0),
                LocalDateTime.of(2026, 7, 27, 10, 0));
    }

    private ClubNoticeAdjacentPost adjacent(Long postId, String title, int hour) {
        return new ClubNoticeAdjacentPost(
                postId,
                title,
                LocalDateTime.of(2026, 7, 27, hour, 0));
    }
}
