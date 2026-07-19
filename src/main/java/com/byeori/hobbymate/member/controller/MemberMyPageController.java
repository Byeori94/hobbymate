package com.byeori.hobbymate.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.member.dto.MemberMyPageResponse;
import com.byeori.hobbymate.member.service.MemberMyPageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/member")
public class MemberMyPageController {

    private final MemberMyPageService memberMyPageService;

    public MemberMyPageController(MemberMyPageService memberMyPageService) {
        this.memberMyPageService = memberMyPageService;
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

    private void clearInvalidAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
