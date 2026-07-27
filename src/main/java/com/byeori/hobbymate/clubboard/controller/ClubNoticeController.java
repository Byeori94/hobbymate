package com.byeori.hobbymate.clubboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeDetailView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeEditView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeListView;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeReturnQuery;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeUpdateRequest;
import com.byeori.hobbymate.clubboard.service.ClubNoticeService;
import com.byeori.hobbymate.common.exception.ClubBoardAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;
import com.byeori.hobbymate.common.exception.ClubNoticeCreationException;
import com.byeori.hobbymate.common.exception.ClubNoticeDetailException;
import com.byeori.hobbymate.common.exception.ClubNoticeDeletionException;
import com.byeori.hobbymate.common.exception.ClubNoticeManagementAccessDeniedException;
import com.byeori.hobbymate.common.exception.ClubNoticeNotFoundException;
import com.byeori.hobbymate.common.exception.ClubNoticeUpdateException;

import jakarta.validation.Valid;

@Controller
public class ClubNoticeController {

    private static final String LIST_VIEW = "clubboard/notice-list";
    private static final String CREATE_VIEW = "clubboard/notice-form";
    private static final String UPDATE_VIEW = "clubboard/notice-edit";
    private static final String DETAIL_VIEW = "clubboard/notice-detail";
    private static final String NOT_FOUND_VIEW = "club/not-found";
    private static final String NOTICE_NOT_FOUND_VIEW = "clubboard/notice-not-found";

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

    @GetMapping("/clubs/{clubId}/notices/new")
    public String creationForm(
            @PathVariable String clubId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Model model) {
        if (!model.containsAttribute("noticeCreateRequest")) {
            model.addAttribute("noticeCreateRequest", new ClubNoticeCreateRequest());
        }
        return creationView(clubId, memberId(userDetails), model);
    }

    @GetMapping("/clubs/{clubId}/notices/{postId}")
    public String detail(
            @PathVariable String clubId,
            @PathVariable String postId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @ModelAttribute ClubNoticeListRequest request,
            Model model) {
        ClubNoticeDetailView view = clubNoticeService.getNoticeDetail(
                clubId, postId, memberId(userDetails), request);
        model.addAttribute("noticeDetailView", view);
        model.addAttribute("activeClubMenu", "NOTICE");
        return DETAIL_VIEW;
    }

    @PostMapping("/clubs/{clubId}/notices")
    public String create(
            @PathVariable String clubId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @Valid @ModelAttribute("noticeCreateRequest") ClubNoticeCreateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long memberId = memberId(userDetails);
        if (bindingResult.hasErrors()) {
            return creationView(clubId, memberId, model);
        }

        try {
            clubNoticeService.createNotice(clubId, memberId, request);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "공지사항이 등록되었습니다.");
            return "redirect:/clubs/" + clubId + "/notices";
        } catch (ClubNoticeCreationException exception) {
            if (exception.isAccessDenied()) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", exception.getMessage());
                return "redirect:/clubs/" + exception.getClubId();
            }
            if (exception.getFieldName() == null) {
                bindingResult.reject("notice.create", exception.getMessage());
            } else {
                bindingResult.rejectValue(
                        exception.getFieldName(),
                        "notice.create." + exception.getFieldName(),
                        exception.getMessage());
            }
            return creationView(clubId, memberId, model);
        }
    }

    @GetMapping("/clubs/{clubId}/notices/{postId}/edit")
    public String updateForm(
            @PathVariable String clubId,
            @PathVariable String postId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @ModelAttribute ClubNoticeListRequest returnRequest,
            Model model) {
        ClubNoticeEditView view = clubNoticeService.prepareUpdate(
                clubId, postId, memberId(userDetails), returnRequest);
        if (!model.containsAttribute("noticeUpdateRequest")) {
            ClubNoticeUpdateRequest request = new ClubNoticeUpdateRequest();
            request.setTitle(view.title());
            request.setContent(view.content());
            request.setPinnedYn(view.pinnedYn());
            model.addAttribute("noticeUpdateRequest", request);
        }
        return updateView(view, model);
    }

    @PostMapping("/clubs/{clubId}/notices/{postId}/edit")
    public String update(
            @PathVariable String clubId,
            @PathVariable String postId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @Valid @ModelAttribute("noticeUpdateRequest") ClubNoticeUpdateRequest request,
            BindingResult bindingResult,
            @ModelAttribute ClubNoticeListRequest returnRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long memberId = memberId(userDetails);
        if (bindingResult.hasErrors()) {
            return updateView(
                    clubNoticeService.prepareUpdate(
                            clubId, postId, memberId, returnRequest),
                    model);
        }

        try {
            ClubNoticeReturnQuery returnQuery = clubNoticeService.updateNotice(
                    clubId, postId, memberId, request, returnRequest);
            addReturnQuery(redirectAttributes, returnQuery);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "공지사항이 수정되었습니다.");
            return "redirect:/clubs/" + clubId + "/notices/" + postId;
        } catch (ClubNoticeUpdateException exception) {
            if (exception.getFieldName() == null) {
                bindingResult.reject("notice.update", exception.getMessage());
            } else {
                bindingResult.rejectValue(
                        exception.getFieldName(),
                        "notice.update." + exception.getFieldName(),
                        exception.getMessage());
            }
            return updateView(
                    clubNoticeService.prepareUpdate(
                            clubId, postId, memberId, returnRequest),
                    model);
        }
    }

    @PostMapping("/clubs/{clubId}/notices/{postId}/delete")
    public String delete(
            @PathVariable String clubId,
            @PathVariable String postId,
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @ModelAttribute ClubNoticeListRequest returnRequest,
            RedirectAttributes redirectAttributes) {
        ClubNoticeReturnQuery returnQuery = clubNoticeService.deleteNotice(
                clubId, postId, memberId(userDetails), returnRequest);
        addReturnQuery(redirectAttributes, returnQuery);
        redirectAttributes.addFlashAttribute(
                "successMessage", "공지사항이 삭제되었습니다.");
        return "redirect:/clubs/" + clubId + "/notices";
    }

    private String creationView(String clubId, Long memberId, Model model) {
        ClubNoticeCreateView view = clubNoticeService.prepareCreation(clubId, memberId);
        model.addAttribute("noticeCreateView", view);
        model.addAttribute("activeClubMenu", "NOTICE");
        return CREATE_VIEW;
    }

    private String updateView(ClubNoticeEditView view, Model model) {
        model.addAttribute("noticeEditView", view);
        model.addAttribute("activeClubMenu", "NOTICE");
        return UPDATE_VIEW;
    }

    private void addReturnQuery(
            RedirectAttributes redirectAttributes,
            ClubNoticeReturnQuery returnQuery) {
        redirectAttributes.addAttribute("page", returnQuery.page());
        redirectAttributes.addAttribute("pageSize", returnQuery.pageSize());
        redirectAttributes.addAttribute("searchType", returnQuery.searchType());
        if (!returnQuery.keyword().isEmpty()) {
            redirectAttributes.addAttribute("keyword", returnQuery.keyword());
        }
    }

    @ExceptionHandler(ClubBoardAccessDeniedException.class)
    public String handleAccessDenied(
            ClubBoardAccessDeniedException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/clubs/" + exception.getClubId();
    }

    @ExceptionHandler(ClubNoticeCreationException.class)
    public String handleCreationAccessDenied(
            ClubNoticeCreationException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        Long clubId = exception.getClubId();
        return clubId == null ? "redirect:/" : "redirect:/clubs/" + clubId;
    }

    @ExceptionHandler(ClubNoticeDetailException.class)
    public String handleDetailFailure(
            ClubNoticeDetailException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        Long clubId = exception.getClubId();
        return clubId == null ? "redirect:/" : "redirect:/clubs/" + clubId + "/notices";
    }

    @ExceptionHandler(ClubNoticeManagementAccessDeniedException.class)
    public String handleManagementAccessDenied(
            ClubNoticeManagementAccessDeniedException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        Long clubId = exception.getClubId();
        return clubId == null ? "redirect:/" : "redirect:/clubs/" + clubId;
    }

    @ExceptionHandler(ClubNoticeDeletionException.class)
    public String handleDeletionFailure(
            ClubNoticeDeletionException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        Long clubId = exception.getClubId();
        Long postId = exception.getPostId();
        if (clubId == null) {
            return "redirect:/";
        }
        return postId == null
                ? "redirect:/clubs/" + clubId + "/notices"
                : "redirect:/clubs/" + clubId + "/notices/" + postId;
    }

    @ExceptionHandler(ClubNoticeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoticeNotFound(
            ClubNoticeNotFoundException exception,
            Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return NOTICE_NOT_FOUND_VIEW;
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
