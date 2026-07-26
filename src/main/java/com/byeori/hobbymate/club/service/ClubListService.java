package com.byeori.hobbymate.club.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.dto.ClubListRequest;
import com.byeori.hobbymate.club.dto.ClubListView;
import com.byeori.hobbymate.club.dto.ClubPage;
import com.byeori.hobbymate.club.dto.ClubSearchCondition;
import com.byeori.hobbymate.club.vo.ClubCategory;
import com.byeori.hobbymate.club.vo.ClubListItem;

@Service
public class ClubListService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_REGION_LENGTH = 100;

    private static final Set<String> SEARCH_TYPES =
            Set.of("ALL", "NAME", "DESCRIPTION", "LEADER");
    private static final Set<String> GENDER_POLICIES =
            Set.of("ALL", "MIXED", "FEMALE", "MALE");
    private static final Set<String> RECRUIT_STATUSES =
            Set.of("ALL", "OPEN", "FULL", "CLOSED");
    private static final Set<String> SORT_TYPES =
            Set.of("RECENT_ACTIVITY", "NEWEST", "MEMBER_COUNT", "AVAILABLE_CAPACITY", "NAME");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);

    private final ClubDao clubDao;

    public ClubListService(ClubDao clubDao) {
        this.clubDao = clubDao;
    }

    @Transactional(readOnly = true)
    public ClubListView getList(ClubListRequest request) {
        List<ClubCategory> categories = clubDao.findActiveCategories();
        List<String> messages = new ArrayList<>();
        ClubSearchCondition search = normalize(request, categories, messages);

        long totalCount = clubDao.countPublicClubs(search);
        int normalizedPage =
                ClubPage.normalizePage(search.page(), totalCount, search.pageSize());
        search = search.withPage(normalizedPage);

        List<ClubListItem> clubs = totalCount == 0
                ? List.of()
                : clubDao.findPublicClubs(search);
        return new ClubListView(
                ClubPage.of(clubs, totalCount, normalizedPage, search.pageSize()),
                search,
                categories,
                messages);
    }

    private ClubSearchCondition normalize(
            ClubListRequest request,
            List<ClubCategory> categories,
            List<String> messages) {
        ClubListRequest source = request == null ? new ClubListRequest() : request;
        String searchType = allowed(source.getSearchType(), SEARCH_TYPES, "ALL");
        String keyword = trim(source.getKeyword());
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            messages.add("검색어는 100자 이하로 입력해 주세요.");
            keyword = null;
        }

        Long requestedCategoryId = parsePositiveLong(source.getCategoryId());
        Long categoryId = requestedCategoryId;
        if (requestedCategoryId != null
                && categories.stream().noneMatch(
                        category -> category.categoryId().equals(requestedCategoryId))) {
            messages.add("존재하지 않는 카테고리 조건은 적용하지 않았습니다.");
            categoryId = null;
        }

        String region = trim(source.getRegion());
        if (region != null && region.length() > MAX_REGION_LENGTH) {
            messages.add("지역 검색어는 100자 이하로 입력해 주세요.");
            region = null;
        }

        String genderPolicy = allowed(source.getGenderPolicy(), GENDER_POLICIES, "ALL");
        String recruitStatus =
                allowed(source.getRecruitStatus(), RECRUIT_STATUSES, "ALL");
        String sortType = allowed(source.getSortType(), SORT_TYPES, "RECENT_ACTIVITY");

        Integer age = parseInteger(source.getAge());
        if (age != null && (age < 18 || age > 100)) {
            messages.add("연령은 만 18세 이상 100세 이하로 입력해 주세요.");
            age = null;
        }

        int page = Math.max(1, parseInteger(source.getPage(), 1));
        int requestedPageSize = parseInteger(source.getPageSize(), DEFAULT_PAGE_SIZE);
        int pageSize = PAGE_SIZES.contains(requestedPageSize)
                ? requestedPageSize
                : DEFAULT_PAGE_SIZE;

        return new ClubSearchCondition(
                searchType,
                valueOrEmpty(keyword),
                likePattern(keyword),
                categoryId,
                valueOrEmpty(region),
                likePattern(region),
                genderPolicy,
                age,
                recruitStatus,
                sortType,
                page,
                pageSize);
    }

    private String allowed(String value, Set<String> allowedValues, String defaultValue) {
        String normalized = trim(value);
        return normalized != null && allowedValues.contains(normalized)
                ? normalized
                : defaultValue;
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

    private Long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseInteger(String value, int defaultValue) {
        Integer parsed = parseInteger(value);
        return parsed == null ? defaultValue : parsed;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
