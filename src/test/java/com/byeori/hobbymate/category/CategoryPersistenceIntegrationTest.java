package com.byeori.hobbymate.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.category.dto.CategoryForm;
import com.byeori.hobbymate.category.service.CategoryService;
import com.byeori.hobbymate.club.dao.ClubDao;

@SpringBootTest
@Transactional
class CategoryPersistenceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ClubDao clubDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void categoryLifecycleImmediatelyControlsClubCreationCategoryList() {
        List<Long> adminIds = jdbcTemplate.queryForList(
                """
                SELECT MEMBER_ID
                FROM HM_MEMBER
                WHERE MEMBER_ROLE IN ('ADMIN', 'SUPER_ADMIN')
                  AND MEMBER_STATUS = 'ACTIVE'
                ORDER BY MEMBER_ID
                LIMIT 1
                """,
                Long.class);
        Assumptions.assumeFalse(adminIds.isEmpty(), "활성 관리자 테스트 계정이 필요합니다.");

        Long adminId = adminIds.getFirst();
        String categoryName = "통합테스트-" + UUID.randomUUID().toString().substring(0, 8);
        CategoryForm form = new CategoryForm();
        form.setCategoryName(categoryName);
        form.setDescription("트랜잭션 롤백되는 카테고리");
        form.setDisplayOrder(1);
        form.setUseYn("Y");

        categoryService.create(adminId, form);
        var created = categoryService.getAdminList(categoryName, "ALL")
                .categories()
                .getFirst();

        assertThat(clubDao.findActiveCategories())
                .anyMatch(category -> category.categoryId().equals(created.categoryId())
                        && category.categoryName().equals(categoryName));

        assertThat(categoryService.toggleStatus(adminId, created.categoryId())).isFalse();
        assertThat(clubDao.findActiveCategories())
                .noneMatch(category -> category.categoryId().equals(created.categoryId()));

        assertThat(categoryService.toggleStatus(adminId, created.categoryId())).isTrue();
        assertThat(clubDao.findActiveCategories())
                .anyMatch(category -> category.categoryId().equals(created.categoryId()));
    }
}
