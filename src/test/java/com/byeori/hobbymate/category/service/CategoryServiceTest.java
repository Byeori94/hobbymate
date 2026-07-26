package com.byeori.hobbymate.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.byeori.hobbymate.category.dao.CategoryDao;
import com.byeori.hobbymate.category.dto.CategoryForm;
import com.byeori.hobbymate.category.dto.CategoryReorderRequest;
import com.byeori.hobbymate.category.dto.CategorySearchCondition;
import com.byeori.hobbymate.category.vo.AdminCategory;
import com.byeori.hobbymate.category.vo.CategoryCommand;
import com.byeori.hobbymate.common.exception.CategoryManagementException;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryDao categoryDao;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryDao);
        lenient().when(categoryDao.isActiveAdmin(1L)).thenReturn(true);
    }

    @Test
    void listNormalizesKeywordAndInvalidUseFilter() {
        when(categoryDao.findAdminCategories(any())).thenReturn(List.of());

        var view = service.getAdminList("  운동  ", "invalid");

        ArgumentCaptor<CategorySearchCondition> captor =
                ArgumentCaptor.forClass(CategorySearchCondition.class);
        verify(categoryDao).findAdminCategories(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("운동");
        assertThat(captor.getValue().useYn()).isEqualTo("ALL");
        assertThat(view.reorderAvailable()).isFalse();
    }

    @Test
    void createTrimsValuesAndUsesAuthenticatedAdministrator() {
        CategoryForm form = validForm();
        form.setCategoryName("  운동  ");
        form.setDescription("  몸을 움직이는 취미  ");
        when(categoryDao.existsName("운동", null)).thenReturn(false);
        when(categoryDao.insertCategory(any())).thenReturn(1);

        service.create(1L, form);

        ArgumentCaptor<CategoryCommand> captor = ArgumentCaptor.forClass(CategoryCommand.class);
        verify(categoryDao).insertCategory(captor.capture());
        assertThat(captor.getValue().adminMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().categoryName()).isEqualTo("운동");
        assertThat(captor.getValue().description()).isEqualTo("몸을 움직이는 취미");
    }

    @Test
    void duplicateNameIsRejectedBeforeInsert() {
        CategoryForm form = validForm();
        when(categoryDao.existsName("운동", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, form))
                .isInstanceOf(CategoryManagementException.class)
                .hasMessage("이미 등록된 카테고리명입니다.");

        verify(categoryDao, never()).insertCategory(any());
    }

    @Test
    void databaseUniqueViolationIsConvertedToFriendlyMessage() {
        CategoryForm form = validForm();
        when(categoryDao.insertCategory(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.create(1L, form))
                .isInstanceOf(CategoryManagementException.class)
                .hasMessage("이미 등록된 카테고리명입니다.");
    }

    @Test
    void editDuplicateCheckExcludesCurrentCategory() {
        CategoryForm form = validForm();
        when(categoryDao.findByIdForUpdate(7L)).thenReturn(category(7L, "기존", "Y", 1));
        when(categoryDao.existsName("운동", 7L)).thenReturn(false);
        when(categoryDao.updateCategory(eq(7L), any())).thenReturn(1);

        service.update(1L, 7L, form);

        verify(categoryDao).existsName("운동", 7L);
        verify(categoryDao).updateCategory(eq(7L), any());
    }

    @Test
    void statusIsToggledFromLockedDatabaseState() {
        when(categoryDao.findByIdForUpdate(7L)).thenReturn(category(7L, "운동", "Y", 1));
        when(categoryDao.updateStatus(7L, "N", 1L)).thenReturn(1);

        assertThat(service.toggleStatus(1L, 7L)).isFalse();

        verify(categoryDao).updateStatus(7L, "N", 1L);
    }

    @Test
    void reorderRequiresEveryCategoryAndNormalizesToConsecutiveOrder() {
        CategoryReorderRequest request = new CategoryReorderRequest();
        request.setCategoryIds(List.of(1L, 2L, 3L));
        request.setDisplayOrders(List.of("30", "10", "10"));
        when(categoryDao.findAllIdsForUpdate()).thenReturn(List.of(1L, 2L, 3L));
        when(categoryDao.updateDisplayOrder(any(), any(Integer.class), eq(1L))).thenReturn(1);

        service.reorder(1L, request);

        InOrder order = inOrder(categoryDao);
        order.verify(categoryDao).updateDisplayOrder(2L, 1, 1L);
        order.verify(categoryDao).updateDisplayOrder(3L, 2, 1L);
        order.verify(categoryDao).updateDisplayOrder(1L, 3, 1L);
    }

    @Test
    void manipulatedPartialReorderIsRejectedBeforeUpdate() {
        CategoryReorderRequest request = new CategoryReorderRequest();
        request.setCategoryIds(List.of(1L));
        request.setDisplayOrders(List.of("1"));
        when(categoryDao.findAllIdsForUpdate()).thenReturn(List.of(1L, 2L));

        assertThatThrownBy(() -> service.reorder(1L, request))
                .isInstanceOf(CategoryManagementException.class)
                .hasMessageContaining("새로고침");

        verify(categoryDao, never()).updateDisplayOrder(any(), any(Integer.class), any());
    }

    @Test
    void nonAdministratorCannotCreateCategory() {
        when(categoryDao.isActiveAdmin(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(2L, validForm()))
                .isInstanceOf(CategoryManagementException.class)
                .hasMessage("관리자 권한이 필요합니다.");

        verify(categoryDao, never()).insertCategory(any());
    }

    private CategoryForm validForm() {
        CategoryForm form = new CategoryForm();
        form.setCategoryName("운동");
        form.setDescription("활동적인 취미");
        form.setDisplayOrder(1);
        form.setUseYn("Y");
        return form;
    }

    private AdminCategory category(Long id, String name, String useYn, int displayOrder) {
        return new AdminCategory(
                id,
                name,
                null,
                displayOrder,
                useYn,
                0,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
