package com.byeori.hobbymate.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.byeori.hobbymate.common.exception.InvalidProfileImageException;
import com.byeori.hobbymate.common.exception.ProfileImageProcessingException;
import com.byeori.hobbymate.file.storage.ProfileImageStorage;
import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;

@ExtendWith(MockitoExtension.class)
class MemberProfileImageServiceTest {

    private static final String OLD_FILE = "11111111-1111-4111-8111-111111111111.jpg";
    private static final String NEW_FILE = "22222222-2222-4222-8222-222222222222.png";

    @Mock
    private MemberDao memberDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfileImageStorage profileImageStorage;

    @InjectMocks
    private MemberMyPageService memberMyPageService;

    @Test
    void storesNewFileUpdatesDatabaseThenDeletesPreviousFile() {
        MockMultipartFile upload = upload();
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(OLD_FILE));
        when(profileImageStorage.store(upload)).thenReturn(NEW_FILE);
        when(memberDao.updateActiveMemberProfileImage(1L, NEW_FILE)).thenReturn(1);

        assertThat(memberMyPageService.updateProfileImage(1L, upload)).isEqualTo(NEW_FILE);

        InOrder order = inOrder(profileImageStorage, memberDao);
        order.verify(profileImageStorage).store(upload);
        order.verify(memberDao).updateActiveMemberProfileImage(1L, NEW_FILE);
        order.verify(profileImageStorage).delete(OLD_FILE);
    }

    @Test
    void databaseFailureDeletesOnlyNewCompensationFile() {
        MockMultipartFile upload = upload();
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(OLD_FILE));
        when(profileImageStorage.store(upload)).thenReturn(NEW_FILE);
        when(memberDao.updateActiveMemberProfileImage(1L, NEW_FILE)).thenReturn(0);

        assertThatThrownBy(() -> memberMyPageService.updateProfileImage(1L, upload))
                .isInstanceOf(ProfileImageProcessingException.class);

        verify(profileImageStorage).delete(NEW_FILE);
        verify(profileImageStorage, never()).delete(OLD_FILE);
    }

    @Test
    void deleteClearsDatabaseBeforeDeletingFile() {
        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(OLD_FILE));
        when(memberDao.updateActiveMemberProfileImage(1L, null)).thenReturn(1);

        memberMyPageService.deleteProfileImage(1L);

        InOrder order = inOrder(memberDao, profileImageStorage);
        order.verify(memberDao).updateActiveMemberProfileImage(1L, null);
        order.verify(profileImageStorage).delete(OLD_FILE);
    }

    @Test
    void missingActiveMemberOrImageCannotBeChanged() {
        assertThatThrownBy(() -> memberMyPageService.updateProfileImage(99L, upload()))
                .isInstanceOf(ProfileImageProcessingException.class);
        verify(profileImageStorage, never()).store(any());

        when(memberDao.findActiveMemberForMyPage(1L)).thenReturn(member(null));
        assertThatThrownBy(() -> memberMyPageService.deleteProfileImage(1L))
                .isInstanceOf(InvalidProfileImageException.class)
                .hasMessage("등록된 프로필 사진이 없습니다.");
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
}
