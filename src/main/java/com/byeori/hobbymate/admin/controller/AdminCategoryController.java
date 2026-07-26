package com.byeori.hobbymate.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.category.dto.CategoryForm;
import com.byeori.hobbymate.category.dto.CategoryListView;
import com.byeori.hobbymate.category.dto.CategoryReorderRequest;
import com.byeori.hobbymate.category.service.CategoryService;
import com.byeori.hobbymate.common.exception.CategoryManagementException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private static final String LIST_VIEW = "admin/categories";
    private static final String FORM_VIEW = "admin/category-form";
    private static final String REDIRECT_LIST = "redirect:/admin/categories";

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String useYn,
            Model model) {
        CategoryListView view = categoryService.getAdminList(keyword, useYn);
        model.addAttribute("categories", view.categories());
        model.addAttribute("search", view.search());
        model.addAttribute("reorderAvailable", view.reorderAvailable());
        model.addAttribute("reorderRequest", new CategoryReorderRequest());
        model.addAttribute("activeAdminMenu", "categories");
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", categoryService.prepareCreateForm());
        }
        prepareFormModel(model, null);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, null);
            return FORM_VIEW;
        }
        try {
            categoryService.create(memberId(principal), form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "카테고리가 등록되었습니다.");
            return REDIRECT_LIST;
        } catch (CategoryManagementException exception) {
            reject(bindingResult, exception);
            prepareFormModel(model, null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{categoryId}/edit")
    public String editForm(@PathVariable Long categoryId, Model model) {
        try {
            if (!model.containsAttribute("categoryForm")) {
                model.addAttribute("categoryForm", categoryService.getEditForm(categoryId));
            }
            prepareFormModel(model, categoryId);
            return FORM_VIEW;
        } catch (CategoryManagementException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return list("", "ALL", model);
        }
    }

    @PostMapping("/{categoryId}")
    public String update(
            @PathVariable Long categoryId,
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, categoryId);
            return FORM_VIEW;
        }
        try {
            categoryService.update(memberId(principal), categoryId, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "카테고리가 수정되었습니다.");
            return REDIRECT_LIST;
        } catch (CategoryManagementException exception) {
            if (exception.isNotFound()) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return REDIRECT_LIST;
            }
            reject(bindingResult, exception);
            prepareFormModel(model, categoryId);
            return FORM_VIEW;
        }
    }

    @PostMapping("/{categoryId}/status")
    public String toggleStatus(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            RedirectAttributes redirectAttributes) {
        try {
            boolean enabled = categoryService.toggleStatus(memberId(principal), categoryId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    enabled
                            ? "카테고리가 다시 사용 설정되었습니다."
                            : "카테고리가 사용 중지되었습니다.");
        } catch (CategoryManagementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/reorder")
    public String reorder(
            @ModelAttribute CategoryReorderRequest request,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            RedirectAttributes redirectAttributes) {
        try {
            categoryService.reorder(memberId(principal), request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "카테고리 노출 순서가 저장되었습니다.");
        } catch (CategoryManagementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return REDIRECT_LIST;
    }

    private void prepareFormModel(Model model, Long categoryId) {
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("editMode", categoryId != null);
        model.addAttribute("activeAdminMenu", "categories");
    }

    private void reject(
            BindingResult bindingResult,
            CategoryManagementException exception) {
        if (exception.getFieldName() == null) {
            bindingResult.reject("category.management", exception.getMessage());
        } else {
            bindingResult.rejectValue(
                    exception.getFieldName(),
                    "category.management." + exception.getFieldName(),
                    exception.getMessage());
        }
    }

    private Long memberId(HobbyMateUserDetails principal) {
        return principal == null ? null : principal.getMemberId();
    }
}
