package com.byeori.hobbymate.common.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.byeori.hobbymate.common.exception.InvalidProfileImageException;
import com.byeori.hobbymate.common.exception.ProfileImageProcessingException;

@ControllerAdvice
public class ProfileImageExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageExceptionHandler.class);

    @ExceptionHandler(InvalidProfileImageException.class)
    public String handleInvalidProfileImage(
            InvalidProfileImageException exception,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        return "redirect:/member/mypage";
    }

    @ExceptionHandler(ProfileImageProcessingException.class)
    public String handleProfileImageProcessing(
            ProfileImageProcessingException exception,
            RedirectAttributes redirectAttributes) {
        log.error("프로필 이미지 처리 실패: {}", exception.getMessage());
        redirectAttributes.addFlashAttribute(
                "errorMessage", "프로필 사진 처리 중 오류가 발생했습니다.");
        return "redirect:/member/mypage";
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handleMultipartLimit(
            Exception exception,
            RedirectAttributes redirectAttributes) {
        log.warn("프로필 이미지 업로드 요청 크기 제한 초과");
        redirectAttributes.addFlashAttribute(
                "errorMessage", "5MB 이하의 이미지만 등록할 수 있습니다.");
        return "redirect:/member/mypage";
    }
}
