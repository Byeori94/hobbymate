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
import org.springframework.web.multipart.MultipartFile;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.common.exception.MemberProfileUpdateException;
import com.byeori.hobbymate.common.exception.MemberPasswordChangeException;
import com.byeori.hobbymate.common.exception.MemberWithdrawalException;
import com.byeori.hobbymate.member.dto.AvailabilityResponse;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.dto.MemberPasswordChangeRequest;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateRequest;
import com.byeori.hobbymate.member.dto.MemberProfileUpdateResult;
import com.byeori.hobbymate.member.dto.MemberWithdrawalRequest;
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

    @GetMapping("/mypage/password")
    public String passwordForm(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            HttpServletRequest request,
            Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        if (memberMyPageService.getMyPage(userDetails.getMemberId()).isEmpty()) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        model.addAttribute("passwordForm", new MemberPasswordChangeRequest());
        return "member/mypage-password";
    }

    @PostMapping("/mypage/password")
    public String changePassword(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Authentication authentication,
            @Valid @ModelAttribute("passwordForm") MemberPasswordChangeRequest passwordForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (!isExpectedAuthentication(userDetails, authentication)) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            passwordForm.clearPasswords();
            return "member/mypage-password";
        }

        try {
            memberMyPageService.changePassword(userDetails.getMemberId(), passwordForm);
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 변경되었습니다.");
            return "redirect:/member/mypage";
        } catch (MemberPasswordChangeException ex) {
            if (ex.getField() == null) {
                bindingResult.reject("password.change.failed", ex.getMessage());
            } else {
                bindingResult.rejectValue(
                        ex.getField(), "password.change.invalid", ex.getMessage());
            }
            passwordForm.clearPasswords();
            return "member/mypage-password";
        }
    }

    @GetMapping("/mypage/withdraw")
    public String withdrawalForm(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            HttpServletRequest request,
            Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        if (memberMyPageService.getMyPage(userDetails.getMemberId()).isEmpty()) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        model.addAttribute("withdrawForm", new MemberWithdrawalRequest());
        return "member/mypage-withdraw";
    }

    @PostMapping("/mypage/withdraw")
    public String withdrawMember(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Authentication authentication,
            @Valid @ModelAttribute("withdrawForm") MemberWithdrawalRequest withdrawForm,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (!isExpectedAuthentication(userDetails, authentication)) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            withdrawForm.clearPassword();
            return "member/mypage-withdraw";
        }

        try {
            memberMyPageService.withdrawMember(userDetails.getMemberId(), withdrawForm);
            redirectAttributes.addFlashAttribute("successMessage", "회원 탈퇴가 완료되었습니다.");
            clearInvalidAuthentication(request);
            return "redirect:/";
        } catch (MemberWithdrawalException ex) {
            if (ex.getField() == null) {
                bindingResult.reject("member.withdraw.failed", ex.getMessage());
            } else {
                bindingResult.rejectValue(
                        ex.getField(), "member.withdraw.invalid", ex.getMessage());
            }
            withdrawForm.clearPassword();
            return "member/mypage-withdraw";
        }
    }

    @PostMapping("/mypage/profile-image")
    public String updateProfileImage(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Authentication authentication,
            @RequestParam("profileImage") MultipartFile profileImage,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (!isExpectedAuthentication(userDetails, authentication)) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        String storedFileName = memberMyPageService.updateProfileImage(
                userDetails.getMemberId(), profileImage);
        updateAuthenticationPrincipal(
                userDetails.withProfileImageUrl(storedFileName), authentication, request);
        redirectAttributes.addFlashAttribute("successMessage", "프로필 사진이 저장되었습니다.");
        return "redirect:/member/mypage";
    }

    @PostMapping("/mypage/profile-image/delete")
    public String deleteProfileImage(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (!isExpectedAuthentication(userDetails, authentication)) {
            clearInvalidAuthentication(request);
            return "redirect:/auth/login";
        }

        memberMyPageService.deleteProfileImage(userDetails.getMemberId());
        updateAuthenticationPrincipal(
                userDetails.withProfileImageUrl(null), authentication, request);
        redirectAttributes.addFlashAttribute("successMessage", "기본 프로필 사진으로 변경되었습니다.");
        return "redirect:/member/mypage";
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
        updateAuthenticationPrincipal(updatedPrincipal, authentication, request);
    }

    private void updateAuthenticationPrincipal(
            HobbyMateUserDetails updatedPrincipal,
            Authentication authentication,
            HttpServletRequest request) {
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
