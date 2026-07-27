package com.byeori.hobbymate.clubboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.club.dto.ClubPage;
import com.byeori.hobbymate.clubboard.dao.ClubNoticeDao;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@Service
public class ClubNoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;
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
