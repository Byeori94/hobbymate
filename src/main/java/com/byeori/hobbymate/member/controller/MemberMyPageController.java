package com.byeori.hobbymate.member.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.common.exception.MemberProfileUpdateException;
import com.byeori.hobbymate.member.dto.AvailabilityResponse;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateRequest;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateResult;
import com.byeori.hobbymate.member.service.MemberMyPageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/member")
public class MemberMyPageController {

    private final MemberMyPageService memberMyPageService;

    public MemberMyPageController(MemberMyPageService memberMyPageService) {
        this.memberMyPageService = memberMyPageService;
    }

    @InitBinder("updateForm")
    void trimUpdateForm(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/mypage")
    public String myPage(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            HttpServletRequest request,
            Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        MemberMyPageResponse member = memberMyPageService.getMyPage(userDetails.getMemberId())
                .orElse(null);
        if (member == null) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        model.addAttribute("member", member);
        return "member/mypage";
    }

    @GetMapping("/mypage/edit")
    public String editForm(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            HttpServletRequest request,
            Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        MemberMyPageResponse member = memberMyPageService.getMyPage(userDetails.getMemberId())
                .orElse(null);
        if (member == null) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        model.addAttribute("member", member);
        model.addAttribute("updateForm", new MemberProfileUpdateRequest(
                member.nickname(), member.email()));
        return "member/mypage-edit";
    }

    @GetMapping("/mypage/check-nickname")
    @ResponseBody
    public AvailabilityResponse checkNickname(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @RequestParam(name = "value", required = false) String value) {
        if (userDetails == null) {
            return new AvailabilityResponse(false, "회원정보를 확인할 수 없습니다.");
        }

        try {
            boolean available = memberMyPageService.isNicknameAvailable(
                    userDetails.getMemberId(), value);
            return new AvailabilityResponse(
                    available,
                    available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.");
        } catch (MemberProfileUpdateException ex) {
            return new AvailabilityResponse(false, ex.getMessage());
        }
    }

    @PostMapping("/mypage/edit")
    public String updateProfile(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Authentication authentication,
            @Valid @ModelAttribute("updateForm") MemberProfileUpdateRequest updateForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!isExpectedAuthentication(userDetails, authentication)) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            return restoreEditForm(userDetails, request, model);
        }

        try {
            MemberProfileUpdateResult result = memberMyPageService.updateProfile(
                    userDetails.getMemberId(), updateForm);
            updateAuthenticationNickname(userDetails, authentication, result.nickname(), request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    result.changed() ? "회원정보가 수정되었습니다." : "변경된 정보가 없습니다.");
            return "redirect:/member/mypage";
        } catch (MemberProfileUpdateException ex) {
            if (ex.getField() == null) {
                bindingResult.reject("profile.update.failed", ex.getMessage());
            } else {
                bindingResult.rejectValue(ex.getField(), "profile.update.invalid", ex.getMessage());
            }
            return restoreEditForm(userDetails, request, model);
        }
    }

    private String restoreEditForm(
            HobbyMateUserDetails userDetails,
            HttpServletRequest request,
            Model model) {
        MemberMyPageResponse member = memberMyPageService.getMyPage(userDetails.getMemberId())
                .orElse(null);
        if (member == null) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }
        model.addAttribute("member", member);
        return "member/mypage-edit";
    }

    private boolean isExpectedAuthentication(
            HobbyMateUserDetails userDetails,
            Authentication authentication) {
        return userDetails != null
                && authentication != null
                && authentication.getPrincipal() instanceof HobbyMateUserDetails principal
                && userDetails.getMemberId().equals(principal.getMemberId());
    }

    private void updateAuthenticationNickname(
            HobbyMateUserDetails userDetails,
            Authentication authentication,
            String nickname,
            HttpServletRequest request) {
        HobbyMateUserDetails updatedPrincipal = userDetails.withNickname(nickname);
        UsernamePasswordAuthenticationToken updatedAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        updatedPrincipal,
                        authentication.getCredentials(),
                        authentication.getAuthorities());
        updatedAuthentication.setDetails(authentication.getDetails());

        var securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(updatedAuthentication);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext);
        }
    }

    private void clearInvalidAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
