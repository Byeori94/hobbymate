package com.byeori.hobbymate.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.byeori.hobbymate.auth.security.HobbyMateUserDetails;
import com.byeori.hobbymate.common.exception.InvalidProfileImageException;
import com.byeori.hobbymate.file.storage.ProfileImageStorage;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;

@SpringBootTest
@AutoConfigureMockMvc
class MemberProfileImageIntegrationTest {

    private static final String OLD_FILE = "11111111-1111-4111-8111-111111111111.jpg";
    private static final String NEW_FILE = "22222222-2222-4222-8222-222222222222.png";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberDao memberDao;

    @MockitoBean
    private ProfileImageStorage profileImageStorage;

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void setUpMember() {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(null));
    }

    @Test
    void anonymousAndCsrfLessUploadsAreBlocked() throws Exception {
        MockMultipartFile file = upload();

        mockMvc.perform(multipart("/member/mypage/profile-image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        mockMvc.perform(multipart("/member/mypage/profile-image")
                        .file(file)
                        .session(authenticatedSession("ROLE_USER")))
                .andExpect(status().isForbidden());

        verify(profileImageStorage, never()).store(any());
    }

    @Test
    void successfulUploadUsesPrincipalAndRefreshesSessionPrincipal() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_USER", "ROLE_ADMIN");
        when(profileImageStorage.store(any())).thenReturn(NEW_FILE);
        when(memberDao.updateActiveMemberProfileImage(1L, NEW_FILE)).thenReturn(1);

        mockMvc.perform(multipart("/member/mypage/profile-image")
                        .file(upload())
                        .param("memberId", "999")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(flash().attribute("successMessage", "프로필 사진이 저장되었습니다."));

        verify(memberDao).updateActiveMemberProfileImage(1L, NEW_FILE);
        HobbyMateUserDetails principal = sessionPrincipal(session);
        assertThat(principal.getProfileImageUrl()).isEqualTo(NEW_FILE);
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void deleteClearsDatabaseAndSessionPrincipal() throws Exception {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(OLD_FILE));
        when(memberDao.updateActiveMemberProfileImage(1L, null)).thenReturn(1);
        MockHttpSession session = authenticatedSessionWithProfile(OLD_FILE, "ROLE_USER");

        mockMvc.perform(post("/member/mypage/profile-image/delete")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(flash().attribute(
                        "successMessage", "기본 프로필 사진으로 변경되었습니다."));

        verify(memberDao).updateActiveMemberProfileImage(1L, null);
        verify(profileImageStorage).delete(OLD_FILE);
        assertThat(sessionPrincipal(session).getProfileImageUrl()).isNull();
    }

    @Test
    void validationErrorUsesSafeFlashMessage() throws Exception {
        when(profileImageStorage.store(any()))
                .thenThrow(new InvalidProfileImageException("올바른 이미지 파일이 아닙니다."));

        mockMvc.perform(multipart("/member/mypage/profile-image")
                        .file(upload())
                        .session(authenticatedSession("ROLE_USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/mypage"))
                .andExpect(flash().attribute("errorMessage", "올바른 이미지 파일이 아닙니다."));

        verify(memberDao, never()).updateActiveMemberProfileImage(any(), any());
    }

    @Test
    void pageUsesContextAwareFormsResourcesAndImageUrls() throws Exception {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(OLD_FILE));

        mockMvc.perform(get("/hobbymate/member/mypage")
                        .contextPath("/hobbymate")
                        .session(authenticatedSessionWithProfile(OLD_FILE, "ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/member/mypage/profile-image\"")))
                .andExpect(content().string(Matchers.containsString(
                        "action=\"/hobbymate/member/mypage/profile-image/delete\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/profile-images/" + OLD_FILE + "\"")))
                .andExpect(content().string(Matchers.containsString(
                        "src=\"/hobbymate/js/pages/mypage.js\"")))
                .andExpect(content().string(Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void profileImageResponseIsPublicTypedAndNosniff() throws Exception {
        Path image = tempDirectory.resolve(NEW_FILE);
        Files.write(image, new byte[] {1, 2, 3});
        when(profileImageStorage.find(NEW_FILE)).thenReturn(java.util.Optional.of(image));
        when(profileImageStorage.mediaType(NEW_FILE)).thenReturn(MediaType.IMAGE_PNG);

        mockMvc.perform(get("/profile-images/{fileName}", NEW_FILE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[] {1, 2, 3}))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    private MockMultipartFile upload() {
        return new MockMultipartFile(
                "profileImage", "photo.png", "image/png", new byte[] {1, 2, 3});
    }

    private MemberMyPageInfo member(String profileImage) {
        return new MemberMyPageInfo(
                "byeori94", "벼리", "최벼리", "sample@example.com", "01012341234",
                "FEMALE", LocalDate.of(1994, 3, 10),
                LocalDateTime.of(2026, 7, 19, 10, 20), profileImage);
    }

    private MockHttpSession authenticatedSession(String... roles) {
        return authenticatedSessionWithProfile(null, roles);
    }

    private MockHttpSession authenticatedSessionWithProfile(String profileImage, String... roles) {
        HobbyMateUserDetails principal = new HobbyMateUserDetails(
                1L, "byeori94", "session-hash", "벼리", profileImage,
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private HobbyMateUserDetails sessionPrincipal(MockHttpSession session) {
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return (HobbyMateUserDetails) context.getAuthentication().getPrincipal();
    }
}
