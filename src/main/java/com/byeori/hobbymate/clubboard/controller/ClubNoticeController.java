package com.byeori.hobbymate.clubboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.service.ClubNoticeService;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@Controller
public class ClubNoticeController {

    private static final String LIST_VIEW = "clubboard/notice-list";
    private static final String NOT_FOUND_VIEW = "club/not-found";

    private final ClubNoticeService clubNoticeService;

    public ClubNoticeController(ClubNoticeService clubNoticeService) {
        this.clubNoticeService = clubNoticeService;
    }

    @GetMapping("/clubs/{clubId}/notices")
    public String notices(
            @PathVariable String clubId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @ModelAttribute ClubNoticeListRequest request,
            Model model) {
        ClubNoticeListView view =
                clubNoticeService.getNotices(clubId, memberId(userDetails), request);
        model.addAttribute("noticeView", view);
        model.addAttribute("activeClubMenu", "NOTICE");
        return LIST_VIEW;
    }

    @ExceptionHandler(ClubBoardAccessDeniedException.class)
    public String handleAccessDenied(
            ClubBoardAccessDeniedException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/clubs/" + exception.getClubId();
    }

    @ExceptionHandler(ClubNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleClubNotFound(ClubNotFoundException exception, Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return NOT_FOUND_VIEW;
    }

    private Long memberId(HobbyMateUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getMemberId();
    }
}
