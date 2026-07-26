package com.byeori.hobbymate.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.dto.ClubCreateRequest;
import com.byeori.hobbymate.club.vo.ClubCategory;
import com.byeori.hobbymate.club.vo.ClubCreation;
import com.byeori.hobbymate.club.vo.ClubCreationMember;
import com.byeori.hobbymate.club.vo.ClubLeaderRegistration;
import com.byeori.hobbymate.common.exception.ClubCreationException;
import com.byeori.hobbymate.file.storage.ClubImageStorage;

@ExtendWith(MockitoExtension.class)
class ClubCreationServiceTest {

    @Mock
    private ClubDao clubDao;

    @Mock
    private ClubImageStorage clubImageStorage;

    private ClubCreationService service;

    @BeforeEach
    void setUp() {
        service = new ClubCreationService(clubDao, clubImageStorage);
    }

    @Test
    void eligibleMemberCanOpenForm() {
        when(clubDao.findMemberForCreation(1L)).thenReturn(activeVerifiedMember());
        when(clubDao.countLedActiveOrBlockedClubs(1L)).thenReturn(4);
        when(clubDao.findActiveCategories()).thenReturn(List.of(new ClubCategory(3L, "독서")));

        assertThat(service.prepareCreation(1L).categories())
                .extracting(ClubCategory::categoryName)
                .containsExactly("독서");
    }

    @Test
    void unverifiedMemberCannotOpenForm() {
        when(clubDao.findMemberForCreation(1L))
                .thenReturn(new ClubCreationMember(1L, "ACTIVE", "N"));

        assertThatThrownBy(() -> service.prepareCreation(1L))
                .isInstanceOf(ClubCreationException.class)
                .hasMessage("본인인증을 완료한 회원만 모임을 개설할 수 있습니다.");

        verify(clubDao, never()).findActiveCategories();
    }

    @Test
    void fifthLedClubBlocksCreation() {
        when(clubDao.findMemberForCreation(1L)).thenReturn(activeVerifiedMember());
        when(clubDao.countLedActiveOrBlockedClubs(1L)).thenReturn(5);

        assertThatThrownBy(() -> service.prepareCreation(1L))
                .isInstanceOf(ClubCreationException.class)
                .hasMessage("개설할 수 있는 모임 수는 최대 5개입니다.");
    }

    @Test
    void clubAndLeaderAreInsertedTogetherWithServerManagedValues() {
        ClubCreateRequest request = validRequest();
        when(clubDao.findMemberForCreationForUpdate(1L)).thenReturn(activeVerifiedMember());
        when(clubDao.countLedActiveOrBlockedClubs(1L)).thenReturn(0);
        when(clubDao.existsActiveCategory(3L)).thenReturn(true);
        when(clubDao.insertClub(any())).thenAnswer(invocation -> {
            ClubCreation club = invocation.getArgument(0);
            club.setClubId(51L);
            return 1;
        });
        when(clubDao.insertLeader(any())).thenReturn(1);

        Long clubId = service.createClub(
                1L,
                request,
                new MockMultipartFile("representativeImage", new byte[0]));

        assertThat(clubId).isEqualTo(51L);
        verify(clubDao).insertLeader(new ClubLeaderRegistration(51L, 1L));
        verify(clubImageStorage, never()).store(any());
    }

    @Test
    void manipulatedCategoryIsRejectedBeforeInsert() {
        ClubCreateRequest request = validRequest();
        request.setCategoryId(999L);
        when(clubDao.findMemberForCreationForUpdate(1L)).thenReturn(activeVerifiedMember());
        when(clubDao.countLedActiveOrBlockedClubs(1L)).thenReturn(0);
        when(clubDao.existsActiveCategory(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.createClub(1L, request, null))
                .isInstanceOf(ClubCreationException.class)
                .hasMessage("현재 사용할 수 없는 카테고리입니다.");

        verify(clubDao, never()).insertClub(any());
    }

    @Test
    void reversedAgeRangeIsRejected() {
        ClubCreateRequest request = validRequest();
        request.setMinAge(60);
        request.setMaxAge(20);
        when(clubDao.findMemberForCreationForUpdate(1L)).thenReturn(activeVerifiedMember());
        when(clubDao.countLedActiveOrBlockedClubs(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.createClub(1L, request, null))
                .isInstanceOf(ClubCreationException.class)
                .hasMessage("최대 연령은 최소 연령보다 작을 수 없습니다.");

        verify(clubDao, never()).insertClub(any());
    }

    @Test
    void leaderInsertFailureFailsWholeServiceTransaction() throws NoSuchMethodException {
        ClubCreateRequest request = validRequest();
        when(clubDao.findMemberForCreationForUpdate(1L)).thenReturn(activeVerifiedMember());
        when(clubDao.countLedActiveOrBlockedClubs(1L)).thenReturn(0);
        when(clubDao.existsActiveCategory(3L)).thenReturn(true);
        when(clubDao.insertClub(any())).thenAnswer(invocation -> {
            ClubCreation club = invocation.getArgument(0);
            club.setClubId(51L);
            return 1;
        });
        when(clubDao.insertLeader(any())).thenReturn(0);

        assertThatThrownBy(() -> service.createClub(1L, request, null))
                .isInstanceOf(ClubCreationException.class)
                .hasMessage("모임장을 등록할 수 없습니다.");

        Transactional transactional = ClubCreationService.class
                .getMethod(
                        "createClub",
                        Long.class,
                        ClubCreateRequest.class,
                        org.springframework.web.multipart.MultipartFile.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
    }

    private ClubCreationMember activeVerifiedMember() {
        return new ClubCreationMember(1L, "ACTIVE", "Y");
    }

    private ClubCreateRequest validRequest() {
        ClubCreateRequest request = new ClubCreateRequest();
        request.setClubName(" 주말 독서 모임 ");
        request.setCategoryId(3L);
        request.setActivityRegion(" 서울 마포구 ");
        request.setClubSummary(" 책을 함께 읽어요 ");
        request.setClubDescription(" 매주 한 권을 정해 편안하게 이야기하는 모임입니다. ");
        request.setGenderPolicy("MIXED");
        request.setMinAge(20);
        request.setMaxAge(60);
        request.setMaxMemberCount(20);
        request.setJoinType("APPROVAL");
        request.setJoinGuide(" 읽고 싶은 책을 알려주세요. ");
        return request;
    }
}
