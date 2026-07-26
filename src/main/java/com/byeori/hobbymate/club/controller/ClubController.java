package com.byeori.hobbymate.club.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.club.dto.ClubCreateRequest;
import com.byeori.hobbymate.club.dto.ClubCreationPage;
import com.byeori.hobbymate.club.dto.ClubListRequest;
import com.byeori.hobbymate.club.dto.ClubListView;
import com.byeori.hobbymate.club.service.ClubCreationService;
import com.byeori.hobbymate.club.service.ClubListService;
import com.byeori.hobbymate.common.exception.ClubCreationException;
import com.byeori.hobbymate.common.exception.ClubImageException;

import jakarta.validation.Valid;

@Controller
public class ClubController {

    private static final String CREATE_VIEW = "club/create";
    private static final String LIST_VIEW = "club/list";

    private final ClubCreationService clubCreationService;
    private final ClubListService clubListService;

    public ClubController(
            ClubCreationService clubCreationService,
            ClubListService clubListService) {
        this.clubCreationService = clubCreationService;
        this.clubListService = clubListService;
    }

    @GetMapping("/clubs")
    public String list(
            @ModelAttribute ClubListRequest request,
            Model model) {
        ClubListView view = clubListService.getList(request);
        model.addAttribute("clubs", view.clubs());
        model.addAttribute("search", view.search());
        model.addAttribute("categories", view.categories());
        model.addAttribute("validationMessages", view.validationMessages());
        return LIST_VIEW;
    }

    @GetMapping("/clubs/new")
    public String creationForm(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            ClubCreationPage page = clubCreationService.prepareCreation(memberId(userDetails));
            if (!model.containsAttribute("clubCreateRequest")) {
                model.addAttribute("clubCreateRequest", new ClubCreateRequest());
            }
            model.addAttribute("categories", page.categories());
            return CREATE_VIEW;
        } catch (ClubCreationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/clubs")
    public String create(
            @AuthenticationPrincipal HobbyMateUserDetails userDetails,
            @Valid @ModelAttribute("clubCreateRequest") ClubCreateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long memberId = memberId(userDetails);
        if (bindingResult.hasErrors()) {
            return creationView(memberId, model, redirectAttributes);
        }

        try {
            clubCreationService.createClub(memberId, request, request.getRepresentativeImage());
            redirectAttributes.addFlashAttribute("successMessage", "모임이 개설되었습니다.");
            return "redirect:/";
        } catch (ClubImageException ex) {
            bindingResult.rejectValue("representativeImage", "club.image", ex.getMessage());
            return creationView(memberId, model, redirectAttributes);
        } catch (ClubCreationException ex) {
            if (ex.isAccessDenied()) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/";
            }
            if (ex.getFieldName() == null) {
                bindingResult.reject("club.create", ex.getMessage());
            } else {
                bindingResult.rejectValue(
                        ex.getFieldName(), "club.create." + ex.getFieldName(), ex.getMessage());
            }
            return creationView(memberId, model, redirectAttributes);
        }
    }

    private String creationView(
            Long memberId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            ClubCreationPage page = clubCreationService.prepareCreation(memberId);
            model.addAttribute("categories", page.categories());
            return CREATE_VIEW;
        } catch (ClubCreationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }
    }

    private Long memberId(HobbyMateUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getMemberId();
    }
}
