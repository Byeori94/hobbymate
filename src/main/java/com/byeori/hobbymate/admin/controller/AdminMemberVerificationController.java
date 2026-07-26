package com.byeori.hobbymate.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.admin.dto.BatchVerificationCancelRequest;
import com.byeori.hobbymate.admin.dto.BatchVerificationRequest;
import com.byeori.hobbymate.admin.dto.TemporaryVerificationRequest;
import com.byeori.hobbymate.admin.dto.VerificationCancelRequest;
import com.byeori.hobbymate.admin.dto.VerificationListView;
import com.byeori.hobbymate.admin.dto.VerificationSearchCondition;
import com.byeori.hobbymate.admin.service.AdminMemberVerificationService;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationState;
import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.common.exception.AdminVerificationException;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/admin/member-verifications")
public class AdminMemberVerificationController {

    private static final String REDIRECT_LIST = "redirect:/admin/member-verifications";

    private final AdminMemberVerificationService service;

    public AdminMemberVerificationController(AdminMemberVerificationService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "1") int authPage,
            @RequestParam(defaultValue = "1") int cancelPage,
            @RequestParam(defaultValue = "ALL") String searchType,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        VerificationListView view = service.getList(
                VerificationSearchCondition.of(searchType, keyword),
                authPage,
                cancelPage);
        model.addAttribute("authTargets", view.authTargets());
        model.addAttribute("cancelTargets", view.cancelTargets());
        model.addAttribute("search", view.search());
        model.addAttribute("batchRequest", new BatchVerificationRequest());
        model.addAttribute("batchCancelRequest", new BatchVerificationCancelRequest());
        return "admin/member-verifications";
    }

    @GetMapping("/{memberId}/temporary")
    public String processingForm(@PathVariable Long memberId, Model model) {
        AdminMemberVerificationState member = service.getProcessingTarget(memberId);
        TemporaryVerificationRequest request = new TemporaryVerificationRequest();
        request.setName(member.name());
        request.setBirthDate(member.birthDate());
        request.setGender(member.gender());
        request.setPhone(member.phone());
        model.addAttribute("member", member);
        model.addAttribute("verificationRequest", request);
        model.addAttribute("cancelMode", false);
        return "admin/member-verification-form";
    }

    @PostMapping("/{memberId}/temporary")
    public String processTemporary(
            @PathVariable Long memberId,
            @Valid @ModelAttribute("verificationRequest") TemporaryVerificationRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return processingFormWithErrors(memberId, request, model, false);
        }
        try {
            int count = service.processTemporary(principal.getMemberId(), memberId, request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원 " + count + "명을 임시 본인인증 완료 상태로 처리했습니다.");
            return REDIRECT_LIST;
        } catch (AdminVerificationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return REDIRECT_LIST;
        }
    }

    @PostMapping("/temporary/batch")
    public String processTemporaryBatch(
            @Valid @ModelAttribute BatchVerificationRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return REDIRECT_LIST;
        }
        try {
            int count = service.processTemporaryBatch(principal.getMemberId(), request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원 " + count + "명을 임시 본인인증 완료 상태로 처리했습니다.");
        } catch (AdminVerificationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return REDIRECT_LIST;
    }

    @GetMapping("/{memberId}/temporary/cancel")
    public String cancelForm(@PathVariable Long memberId, Model model) {
        model.addAttribute("member", service.getCancelTarget(memberId));
        model.addAttribute("cancelRequest", new VerificationCancelRequest());
        model.addAttribute("cancelMode", true);
        return "admin/member-verification-form";
    }

    @PostMapping("/{memberId}/temporary/cancel")
    public String cancelTemporary(
            @PathVariable Long memberId,
            @Valid @ModelAttribute("cancelRequest") VerificationCancelRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return cancelFormWithErrors(memberId, request, model);
        }
        try {
            int count = service.cancelTemporary(principal.getMemberId(), memberId, request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원 " + count + "명의 임시 본인인증을 취소했습니다.");
            return REDIRECT_LIST;
        } catch (AdminVerificationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return REDIRECT_LIST;
        }
    }

    @PostMapping("/temporary/cancel/batch")
    public String cancelTemporaryBatch(
            @Valid @ModelAttribute BatchVerificationCancelRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal HobbyMateUserDetails principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(bindingResult));
            return REDIRECT_LIST;
        }
        try {
            int count = service.cancelTemporaryBatch(principal.getMemberId(), request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원 " + count + "명의 임시 본인인증을 취소했습니다.");
        } catch (AdminVerificationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return REDIRECT_LIST;
    }

    private String processingFormWithErrors(
            Long memberId,
            TemporaryVerificationRequest request,
            Model model,
            boolean cancelMode) {
        model.addAttribute("member", service.getProcessingTarget(memberId));
        model.addAttribute("verificationRequest", request);
        model.addAttribute("cancelMode", cancelMode);
        return "admin/member-verification-form";
    }

    private String cancelFormWithErrors(
            Long memberId,
            VerificationCancelRequest request,
            Model model) {
        model.addAttribute("member", service.getCancelTarget(memberId));
        model.addAttribute("cancelRequest", request);
        model.addAttribute("cancelMode", true);
        return "admin/member-verification-form";
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청값을 확인해주세요.");
    }

    @ExceptionHandler(AdminVerificationException.class)
    public String handleVerificationException(
            AdminVerificationException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return REDIRECT_LIST;
    }
}
