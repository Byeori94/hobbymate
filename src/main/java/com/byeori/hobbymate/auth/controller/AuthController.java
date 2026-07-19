package com.byeori.hobbymate.auth.controller;

import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.auth.security.SavedIdAuthenticationSuccessHandler;
import com.byeori.hobbymate.common.exception.MemberJoinException;
import com.byeori.hobbymate.member.dto.AvailabilityResponse;
import com.byeori.hobbymate.member.dto.MemberJoinRequest;
import com.byeori.hobbymate.member.service.MemberJoinService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{4,19}$");

    private final MemberJoinService memberJoinService;

    public AuthController(MemberJoinService memberJoinService) {
        this.memberJoinService = memberJoinService;
    }

    @GetMapping("/login")
    public String loginForm(
            @CookieValue(name = SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME,
                    required = false) String savedLoginId,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "joined", required = false) String joined,
            Model model) {

        boolean hasSavedLoginId = savedLoginId != null
                && LOGIN_ID_PATTERN.matcher(savedLoginId).matches();
        model.addAttribute("savedLoginId", hasSavedLoginId ? savedLoginId : "");
        model.addAttribute("savedIdPresent", hasSavedLoginId);
        model.addAttribute("loginError", error != null);
        model.addAttribute("logoutSuccess", logout != null);
        model.addAttribute("joinSuccess", joined != null);
        return "auth/login";
    }

    @GetMapping("/join")
    public String joinForm(Model model) {
        if (!model.containsAttribute("joinRequest")) {
            model.addAttribute("joinRequest", new MemberJoinRequest());
        }
        model.addAttribute("today", LocalDate.now());
        return "auth/join";
    }

    @PostMapping("/join")
    public String join(
            @Valid @ModelAttribute("joinRequest") MemberJoinRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (containsUppercase(request.getLoginId())) {
            bindingResult.rejectValue("loginId", "loginId.uppercase",
                    "아이디에는 영문 대문자를 사용할 수 없습니다.");
        }

        if (bindingResult.hasErrors()) {
            request.clearPasswords();
            model.addAttribute("today", LocalDate.now());
            return "auth/join";
        }

        try {
            memberJoinService.join(request);
        } catch (MemberJoinException ex) {
            if (ex.getField() == null) {
                bindingResult.reject("join.failed", ex.getMessage());
            } else {
                bindingResult.rejectValue(ex.getField(), "join.invalid", ex.getMessage());
            }
            request.clearPasswords();
            model.addAttribute("today", LocalDate.now());
            return "auth/join";
        } catch (DataIntegrityViolationException ex) {
            bindingResult.reject("join.failed", "입력한 정보가 이미 사용 중이거나 저장할 수 없습니다.");
            request.clearPasswords();
            model.addAttribute("today", LocalDate.now());
            return "auth/join";
        }

        redirectAttributes.addFlashAttribute("joinSuccessMessage",
                "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/auth/login?joined";
    }

    @GetMapping("/check-id")
    @ResponseBody
    public AvailabilityResponse checkLoginId(@RequestParam(name = "value", required = false) String value) {
        try {
            boolean available = memberJoinService.isLoginIdAvailable(value);
            return new AvailabilityResponse(available,
                    available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.");
        } catch (MemberJoinException ex) {
            return new AvailabilityResponse(false, ex.getMessage());
        }
    }

    @GetMapping("/check-nickname")
    @ResponseBody
    public AvailabilityResponse checkNickname(@RequestParam(name = "value", required = false) String value) {
        try {
            boolean available = memberJoinService.isNicknameAvailable(value);
            return new AvailabilityResponse(available,
                    available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.");
        } catch (MemberJoinException ex) {
            return new AvailabilityResponse(false, ex.getMessage());
        }
    }

    @GetMapping("/check-email")
    @ResponseBody
    public AvailabilityResponse checkEmail(@RequestParam(name = "value", required = false) String value) {
        try {
            boolean available = memberJoinService.isEmailAvailable(value);
            return new AvailabilityResponse(available,
                    available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.");
        } catch (MemberJoinException ex) {
            return new AvailabilityResponse(false, ex.getMessage());
        }
    }

    private boolean containsUppercase(String value) {
        return value != null && value.chars().anyMatch(Character::isUpperCase);
    }
}
