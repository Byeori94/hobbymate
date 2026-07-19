package com.byeori.hobbymate.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.Cookie;

class SavedIdAuthenticationSuccessHandlerTest {

    private final SavedIdAuthenticationSuccessHandler handler =
            new SavedIdAuthenticationSuccessHandler();

    @Test
    void savesOnlyLoginIdForThirtyDaysUsingCurrentContextPath() throws Exception {
        MockHttpServletRequest request = requestWithContextPath();
        request.setSecure(true);
        request.addParameter("saveId", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication("member1"));

        Cookie cookie = response.getCookie(SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("member1");
        assertThat(cookie.getMaxAge()).isEqualTo(60 * 60 * 24 * 30);
        assertThat(cookie.getPath()).isEqualTo("/hobbymate");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(response.getRedirectedUrl()).isEqualTo("/hobbymate/");
    }

    @Test
    void uncheckedSaveIdDeletesCookieUsingSameContextPath() throws Exception {
        MockHttpServletRequest request = requestWithContextPath();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication("member1"));

        Cookie cookie = response.getCookie(SavedIdAuthenticationSuccessHandler.SAVED_ID_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/hobbymate");
    }

    private MockHttpServletRequest requestWithContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/hobbymate/auth/login");
        request.setContextPath("/hobbymate");
        return request;
    }

    private Authentication authentication(String loginId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(loginId);
        return authentication;
    }
}
