package com.byeori.hobbymate.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.category.dto.CategoryForm;
import com.byeori.hobbymate.category.dto.CategoryListView;
import com.byeori.hobbymate.category.dto.CategorySearchCondition;
import com.byeori.hobbymate.category.service.CategoryService;
import com.byeori.hobbymate.category.vo.AdminCategory;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCategorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void regularMemberIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/categories").with(user(userDetails("USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanOpenEmptyListAndSeeMenu() throws Exception {
        CategorySearchCondition search = CategorySearchCondition.of("", "ALL");
        when(categoryService.getAdminList("", "ALL"))
                .thenReturn(new CategoryListView(List.of(), search, true));

        mockMvc.perform(get("/admin/categories").with(user(userDetails("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "모임 카테고리 관리")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "등록되거나 조건에 맞는 카테고리가 없습니다.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "카테고리 관리")));
    }

    @Test
    void superAdministratorWithInheritedAdminRoleCanOpenPage() throws Exception {
        CategorySearchCondition search = CategorySearchCondition.of("", "ALL");
        when(categoryService.getAdminList("", "ALL"))
                .thenReturn(new CategoryListView(List.of(), search, true));

        mockMvc.perform(get("/admin/categories").with(user(userDetails("SUPER_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void categoryRowRendersStatusOrderCountAndActions() throws Exception {
        CategorySearchCondition search = CategorySearchCondition.of("", "ALL");
        AdminCategory category = new AdminCategory(
                7L,
                "운동",
                "몸을 움직이는 취미",
                2,
                "Y",
                3,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 2, 11, 0));
        when(categoryService.getAdminList("", "ALL"))
                .thenReturn(new CategoryListView(List.of(category), search, true));

        mockMvc.perform(get("/admin/categories").with(user(userDetails("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("운동")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("사용 중")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "name=\"displayOrders\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/admin/categories/7/edit")));
    }

    @Test
    void mutationWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/admin/categories/7/status")
                        .with(user(userDetails("ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void statusMutationWithCsrfUsesAuthenticatedAdministratorId() throws Exception {
        when(categoryService.toggleStatus(1L, 7L)).thenReturn(false);

        mockMvc.perform(post("/admin/categories/7/status")
                        .with(user(userDetails("ADMIN")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        verify(categoryService).toggleStatus(1L, 7L);
    }

    @Test
    void blankCategoryNameReturnsFieldValidationInsteadOfServerError() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(user(userDetails("ADMIN")))
                        .with(csrf())
                        .param("categoryName", " ")
                        .param("description", "")
                        .param("displayOrder", "1")
                        .param("useYn", "Y"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "카테고리명을 입력해주세요.")));
    }

    @Test
    void successfulCreateUsesPrg() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(user(userDetails("ADMIN")))
                        .with(csrf())
                        .param("categoryName", "운동")
                        .param("description", "활동적인 취미")
                        .param("displayOrder", "1")
                        .param("useYn", "Y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        verify(categoryService).create(eq(1L), any(CategoryForm.class));
    }

    private HobbyMateUserDetails userDetails(String role) {
        List<SimpleGrantedAuthority> authorities = switch (role) {
            case "ADMIN" -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "SUPER_ADMIN" -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            default -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
        };
        return new HobbyMateUserDetails(
                "USER".equals(role) ? 2L : 1L,
                "USER".equals(role) ? "member1" : "admin1",
                "password",
                "USER".equals(role) ? "회원" : "관리자",
                null,
                authorities);
    }
}
