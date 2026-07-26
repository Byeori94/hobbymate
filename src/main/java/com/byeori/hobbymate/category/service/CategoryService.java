package com.byeori.hobbymate.category.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.category.dao.CategoryDao;
import com.byeori.hobbymate.category.dto.CategoryForm;
import com.byeori.hobbymate.category.dto.CategoryListView;
import com.byeori.hobbymate.category.dto.CategoryReorderRequest;
import com.byeori.hobbymate.category.dto.CategorySearchCondition;
import com.byeori.hobbymate.category.vo.AdminCategory;
import com.byeori.hobbymate.category.vo.CategoryCommand;
import com.byeori.hobbymate.common.exception.CategoryManagementException;

@Service
public class CategoryService {

    private static final Set<String> USE_VALUES = Set.of("Y", "N");

    private final CategoryDao categoryDao;

    public CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Transactional(readOnly = true)
    public CategoryListView getAdminList(String keyword, String useYn) {
        CategorySearchCondition search = CategorySearchCondition.of(keyword, useYn);
        return new CategoryListView(
                categoryDao.findAdminCategories(search),
                search,
                search.isDefault());
    }

    @Transactional(readOnly = true)
    public CategoryForm prepareCreateForm() {
        CategoryForm form = new CategoryForm();
        form.setDisplayOrder(categoryDao.findNextDisplayOrder());
        return form;
    }

    @Transactional(readOnly = true)
    public CategoryForm getEditForm(Long categoryId) {
        AdminCategory category = requireCategory(categoryDao.findById(categoryId));
        CategoryForm form = new CategoryForm();
        form.setCategoryName(category.categoryName());
        form.setDescription(category.description());
        form.setDisplayOrder(category.displayOrder());
        form.setUseYn(category.useYn());
        return form;
    }

    @Transactional
    public void create(Long adminMemberId, CategoryForm form) {
        requireAdmin(adminMemberId);
        CategoryCommand command = normalizeAndValidate(adminMemberId, form);
        ensureUniqueName(command.categoryName(), null);
        try {
            if (categoryDao.insertCategory(command) != 1) {
                throw new CategoryManagementException("카테고리를 등록할 수 없습니다.");
            }
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    @Transactional
    public void update(Long adminMemberId, Long categoryId, CategoryForm form) {
        requireAdmin(adminMemberId);
        requireCategory(categoryDao.findByIdForUpdate(categoryId));
        CategoryCommand command = normalizeAndValidate(adminMemberId, form);
        ensureUniqueName(command.categoryName(), categoryId);
        try {
            if (categoryDao.updateCategory(categoryId, command) != 1) {
                throw changedState();
            }
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    @Transactional
    public boolean toggleStatus(Long adminMemberId, Long categoryId) {
        requireAdmin(adminMemberId);
        AdminCategory current = requireCategory(categoryDao.findByIdForUpdate(categoryId));
        String nextUseYn = current.isUsed() ? "N" : "Y";
        if (categoryDao.updateStatus(categoryId, nextUseYn, adminMemberId) != 1) {
            throw changedState();
        }
        return "Y".equals(nextUseYn);
    }

    @Transactional
    public void reorder(Long adminMemberId, CategoryReorderRequest request) {
        requireAdmin(adminMemberId);
        List<Long> categoryIds = request == null ? null : request.getCategoryIds();
        List<String> displayOrders = request == null ? null : request.getDisplayOrders();
        if (categoryIds == null || categoryIds.isEmpty()
                || displayOrders == null || categoryIds.size() != displayOrders.size()) {
            throw new CategoryManagementException("저장할 카테고리 순서를 확인해주세요.");
        }

        Set<Long> uniqueIds = new HashSet<>(categoryIds);
        if (uniqueIds.size() != categoryIds.size() || uniqueIds.contains(null)) {
            throw changedState();
        }

        List<Long> currentIds = categoryDao.findAllIdsForUpdate();
        if (currentIds.size() != uniqueIds.size() || !uniqueIds.containsAll(currentIds)) {
            throw changedState();
        }

        List<OrderCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < categoryIds.size(); index++) {
            int requestedOrder = parseOrder(displayOrders.get(index));
            candidates.add(new OrderCandidate(categoryIds.get(index), requestedOrder));
        }
        candidates.sort(Comparator.comparingInt(OrderCandidate::requestedOrder)
                .thenComparing(OrderCandidate::categoryId));

        for (int index = 0; index < candidates.size(); index++) {
            OrderCandidate candidate = candidates.get(index);
            if (categoryDao.updateDisplayOrder(
                    candidate.categoryId(), index + 1, adminMemberId) != 1) {
                throw changedState();
            }
        }
    }

    private CategoryCommand normalizeAndValidate(Long adminMemberId, CategoryForm form) {
        if (form == null) {
            throw new CategoryManagementException("입력값을 확인해주세요.");
        }
        String categoryName = trimToNull(form.getCategoryName());
        String description = trimToNull(form.getDescription());
        String useYn = form.getUseYn() == null ? null : form.getUseYn().trim().toUpperCase();

        form.setCategoryName(categoryName);
        form.setDescription(description);
        form.setUseYn(useYn);

        if (categoryName == null || categoryName.length() < 2 || categoryName.length() > 50) {
            throw new CategoryManagementException(
                    "categoryName",
                    "카테고리명은 2자 이상 50자 이하로 입력해주세요.");
        }
        if (description != null && description.length() > 200) {
            throw new CategoryManagementException(
                    "description",
                    "설명은 200자 이하로 입력해주세요.");
        }
        if (form.getDisplayOrder() == null || form.getDisplayOrder() < 1) {
            throw new CategoryManagementException(
                    "displayOrder",
                    "노출 순서는 1 이상의 정수로 입력해주세요.");
        }
        if (!USE_VALUES.contains(useYn)) {
            throw new CategoryManagementException(
                    "useYn",
                    "올바른 사용 여부를 선택해주세요.");
        }
        return new CategoryCommand(
                adminMemberId,
                categoryName,
                description,
                form.getDisplayOrder(),
                useYn);
    }

    private void ensureUniqueName(String categoryName, Long excludedCategoryId) {
        if (categoryDao.existsName(categoryName, excludedCategoryId)) {
            throw duplicateName();
        }
    }

    private int parseOrder(String rawOrder) {
        try {
            int order = Integer.parseInt(rawOrder == null ? "" : rawOrder.trim());
            if (order < 1) {
                throw new NumberFormatException();
            }
            return order;
        } catch (NumberFormatException exception) {
            throw new CategoryManagementException(
                    "노출 순서는 1 이상의 정수로 입력해주세요.");
        }
    }

    private void requireAdmin(Long adminMemberId) {
        if (adminMemberId == null || !categoryDao.isActiveAdmin(adminMemberId)) {
            throw new CategoryManagementException("관리자 권한이 필요합니다.");
        }
    }

    private AdminCategory requireCategory(AdminCategory category) {
        if (category == null) {
            throw CategoryManagementException.notFound();
        }
        return category;
    }

    private CategoryManagementException duplicateName() {
        return new CategoryManagementException(
                "categoryName",
                "이미 등록된 카테고리명입니다.");
    }

    private CategoryManagementException changedState() {
        return new CategoryManagementException(
                "카테고리 정보가 변경되었습니다. 목록을 새로고침한 뒤 다시 시도해주세요.");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record OrderCandidate(Long categoryId, int requestedOrder) {
    }
}
