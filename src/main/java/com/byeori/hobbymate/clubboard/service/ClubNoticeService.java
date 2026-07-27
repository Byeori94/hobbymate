package com.byeori.hobbymate.clubboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.club.dto.ClubPage;
import com.byeori.hobbymate.clubboard.dao.ClubNoticeDao;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.clubboard.vo.ClubPostCreation;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;
import com.byeori.hobbymate.common.exception.ClubNoticeCreationException;

@Service
public class ClubNoticeService {

    private static final Logger log = LoggerFactory.getLogger(ClubNoticeService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_PINNED_POSTS = 5;
    private static final String NOTICE_POST_TYPE = "NOTICE";
    private static final Set<String> SEARCH_TYPES =
            Set.of("TITLE", "CONTENT", "TITLE_CONTENT", "WRITER");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);

    private final ClubNoticeDao clubNoticeDao;

    public ClubNoticeService(ClubNoticeDao clubNoticeDao) {
        this.clubNoticeDao = clubNoticeDao;
    }

    @Transactional(readOnly = true)
    public ClubNoticeListView getNotices(
            String rawClubId,
            Long memberId,
            ClubNoticeListRequest request) {
        Long clubId = parseClubId(rawClubId);
        if (memberId == null) {
            throw new ClubBoardAccessDeniedException(clubId);
        }

        ClubBoardAccess access = clubNoticeDao.findClubBoardAccess(clubId, memberId);
        if (access == null) {
            throw new ClubNotFoundException();
        }
        if (!access.isActiveMember()) {
            throw new ClubBoardAccessDeniedException(clubId);
        }

        List<String> validationMessages = new ArrayList<>();
        ClubNoticeSearchCondition search = normalize(clubId, request, validationMessages);
        long totalCount = clubNoticeDao.countNotices(search);
        int normalizedPage =
                ClubPage.normalizePage(search.page(), totalCount, search.pageSize());
        search = search.withPage(normalizedPage);

        List<ClubNoticeListItem> notices = totalCount == 0
                ? List.of()
                : withDisplayNumbers(
                        clubNoticeDao.findNotices(search),
                        totalCount,
                        search.offset());
        return new ClubNoticeListView(
                access.clubId(),
                access.clubName(),
                access.memberRole(),
                access.canManageClub(),
                access.canManageClub(),
                ClubPage.of(notices, totalCount, normalizedPage, search.pageSize()),
                search,
                validationMessages);
    }

    @Transactional(readOnly = true)
    public ClubNoticeCreateView prepareCreation(String rawClubId, Long memberId) {
        Long clubId = parseClubId(rawClubId);
        ClubBoardAccess access = requireNoticeWriter(clubId, memberId);
        return createView(access);
    }

    @Transactional
    public Long createNotice(
            String rawClubId,
            Long memberId,
            ClubNoticeCreateRequest request) {
        Long clubId = parseClubId(rawClubId);
        if (memberId == null) {
            throw ClubNoticeCreationException.accessDenied(clubId);
        }

        requireNoticeWriter(clubId, memberId);
        Long lockedClubId = clubNoticeDao.lockClubForPostCreation(clubId);
        if (lockedClubId == null) {
            throw new ClubNotFoundException();
        }
        ClubBoardAccess access = requireNoticeWriter(clubId, memberId);

        String title = normalizeTitle(request == null ? null : request.getTitle());
        String content = normalizeContent(request == null ? null : request.getContent());
        String pinnedYn = normalizePinnedYn(request == null ? null : request.getPinnedYn());

        if ("Y".equals(pinnedYn)
                && clubNoticeDao.countPinnedPosts(clubId, NOTICE_POST_TYPE)
                >= MAX_PINNED_POSTS) {
            throw new ClubNoticeCreationException(
                    "pinnedYn",
                    "상단 고정 게시글은 게시판별로 최대 5개까지 등록할 수 있습니다.");
        }

        ClubPostCreation post = new ClubPostCreation(
                access.clubId(),
                memberId,
                NOTICE_POST_TYPE,
                title,
                content,
                pinnedYn);
        try {
            if (clubNoticeDao.insertClubPost(post) != 1 || post.getPostId() == null) {
                log.warn(
                        "공지사항 INSERT 결과가 올바르지 않습니다. clubId={}, memberId={}",
                        clubId,
                        memberId);
                throw new ClubNoticeCreationException(
                        null, "공지사항을 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            }
        } catch (DataAccessException exception) {
            log.error(
                    "공지사항 INSERT 중 DB 오류가 발생했습니다. clubId={}, memberId={}",
                    clubId,
                    memberId,
                    exception);
            throw new ClubNoticeCreationException(
                    "공지사항을 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                    exception);
        }
        return post.getPostId();
    }

    private ClubNoticeCreateView createView(ClubBoardAccess access) {
        return new ClubNoticeCreateView(
                access.clubId(),
                access.clubName(),
                access.canManageClub(),
                access.canManageClub());
    }

    private ClubBoardAccess requireNoticeWriter(Long clubId, Long memberId) {
        if (memberId == null) {
            throw ClubNoticeCreationException.accessDenied(clubId);
        }
        ClubBoardAccess access = clubNoticeDao.findClubBoardAccess(clubId, memberId);
        if (access == null) {
            throw new ClubNotFoundException();
        }
        if (!access.canManageClub()) {
            throw ClubNoticeCreationException.accessDenied(clubId);
        }
        return access;
    }

    private String normalizeTitle(String value) {
        String title = trim(value);
        if (title == null) {
            throw new ClubNoticeCreationException("title", "제목을 입력해 주세요.");
        }
        if (title.length() < 2) {
            throw new ClubNoticeCreationException("title", "제목은 2자 이상 입력해 주세요.");
        }
        if (title.length() > 200) {
            throw new ClubNoticeCreationException("title", "제목은 200자 이하로 입력해 주세요.");
        }
        return title;
    }

    private String normalizeContent(String value) {
        String content = trim(value);
        if (content == null) {
            throw new ClubNoticeCreationException("content", "내용을 입력해 주세요.");
        }
        if (content.length() < 10) {
            throw new ClubNoticeCreationException("content", "내용은 10자 이상 입력해 주세요.");
        }
        if (content.length() > 10000) {
            throw new ClubNoticeCreationException(
                    "content", "내용은 10,000자 이하로 입력해 주세요.");
        }
        return content;
    }

    private String normalizePinnedYn(String value) {
        if (value == null) {
            return "N";
        }
        String pinnedYn = value.trim();
        if (!"Y".equals(pinnedYn) && !"N".equals(pinnedYn)) {
            throw new ClubNoticeCreationException(
                    "pinnedYn", "상단 고정 값이 올바르지 않습니다.");
        }
        return pinnedYn;
    }

    private ClubNoticeSearchCondition normalize(
            Long clubId,
            ClubNoticeListRequest request,
            List<String> validationMessages) {
        ClubNoticeListRequest source = request == null
                ? new ClubNoticeListRequest()
                : request;
        String searchType = allowedSearchType(source.getSearchType());
        String keyword = trim(source.getKeyword());
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            validationMessages.add("검색어는 100자 이하로 입력해 주세요.");
            keyword = null;
        }

        int page = Math.max(1, parseInteger(source.getPage(), 1));
        int requestedPageSize =
                parseInteger(source.getPageSize(), DEFAULT_PAGE_SIZE);
        int pageSize = PAGE_SIZES.contains(requestedPageSize)
                ? requestedPageSize
                : DEFAULT_PAGE_SIZE;
        return new ClubNoticeSearchCondition(
                clubId,
                searchType,
                keyword == null ? "" : keyword,
                likePattern(keyword),
                page,
                pageSize);
    }

    private List<ClubNoticeListItem> withDisplayNumbers(
            List<ClubNoticeListItem> notices,
            long totalCount,
            int offset) {
        List<ClubNoticeListItem> numbered = new ArrayList<>(notices.size());
        for (int index = 0; index < notices.size(); index++) {
            long displayNumber = totalCount - offset - index;
            numbered.add(notices.get(index).withDisplayNumber(displayNumber));
        }
        return List.copyOf(numbered);
    }

    private String allowedSearchType(String value) {
        String normalized = trim(value);
        return normalized != null && SEARCH_TYPES.contains(normalized)
                ? normalized
                : "TITLE_CONTENT";
    }

    private String likePattern(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private int parseInteger(String value, int defaultValue) {
        try {
            return value == null || value.isBlank()
                    ? defaultValue
                    : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Long parseClubId(String value) {
        try {
            long clubId = Long.parseLong(value);
            if (clubId < 1) {
                throw new ClubNotFoundException();
            }
            return clubId;
        } catch (NumberFormatException ex) {
            throw new ClubNotFoundException();
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
