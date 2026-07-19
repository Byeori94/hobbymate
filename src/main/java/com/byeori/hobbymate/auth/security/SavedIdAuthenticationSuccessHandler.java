package com.byeori.hobbymate.auth.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SavedIdAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String SAVED_ID_COOKIE_NAME = "hobbymate_saved_id";
    private static final int COOKIE_MAX_AGE_SECONDS = (int) Duration.ofDays(30).toSeconds();

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        updateSavedIdCookie(request, response, authentication.getName());

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, "/");
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void updateSavedIdCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String loginId) {

        boolean saveId = "true".equals(request.getParameter("saveId"));
        Cookie cookie = new Cookie(SAVED_ID_COOKIE_NAME, saveId ? loginId : "");
        cookie.setPath(cookiePath(request));
        cookie.setMaxAge(saveId ? COOKIE_MAX_AGE_SECONDS : 0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String cookiePath(HttpServletRequest request) {
        String contextPath = contextPath(request);
        return contextPath.isEmpty() ? "/" : contextPath;
    }

    private String contextPath(HttpServletRequest request) {
        return request.getContextPath() == null ? "" : request.getContextPath();
    }
}
