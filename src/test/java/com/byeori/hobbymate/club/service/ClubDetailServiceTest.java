package com.byeori.hobbymate.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.dto.ClubDetailView;
import com.byeori.hobbymate.club.vo.ClubDetail;
import com.byeori.hobbymate.club.vo.ClubMemberRelation;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@ExtendWith(MockitoExtension.class)
class ClubDetailServiceTest {

    @Mock
    private ClubDao clubDao;

    private ClubDetailService clubDetailService;

    @BeforeEach
    void setUp() {
        clubDetailService = new ClubDetailService(clubDao);
    }

    @Test
    void anonymousUserOnlyLoadsPublicDetail() {
        when(clubDao.findPublicClubDetail(1L)).thenReturn(detail("OPEN", "APPROVAL"));

        ClubDetailView view = clubDetailService.getDetail("1", null);

        assertThat(view.relationshipType()).isEqualTo("ANONYMOUS");
        assertThat(view.actionLabel()).isEqualTo("로그인");
        verify(clubDao, never()).findClubMemberRelation(1L, null);
    }

    @Test
    void activeLeaderIsRecognizedFromServerSideRelation() {
        when(clubDao.findPublicClubDetail(1L)).thenReturn(detail("OPEN", "APPROVAL"));
        when(clubDao.findClubMemberRelation(1L, 7L))
                .thenReturn(new ClubMemberRelation("LEADER", "ACTIVE", "N", "N", null));

        ClubDetailView view = clubDetailService.getDetail("1", 7L);

        assertThat(view.relationshipType()).isEqualTo("LEADER");
        assertThat(view.relationshipLabel()).isEqualTo("모임장");
        assertThat(view.actionLabel()).isEqualTo("모임 정보 수정");
    }

    @Test
    void permanentBanTakesPriorityOverPreviousMembership() {
        when(clubDao.findPublicClubDetail(1L)).thenReturn(detail("OPEN", "IMMEDIATE"));
        when(clubDao.findClubMemberRelation(1L, 7L))
                .thenReturn(new ClubMemberRelation(
                        "MEMBER", "KICKED", "N", "Y", LocalDateTime.now().minusHours(1)));

        ClubDetailView view = clubDetailService.getDetail("1", 7L);

        assertThat(view.relationshipType()).isEqualTo("BANNED");
        assertThat(view.actionMessage()).contains("다시 가입할 수 없습니다");
    }

    @Test
    void closedRecruitmentRemainsViewableButCannotJoin() {
        when(clubDao.findPublicClubDetail(1L)).thenReturn(detail("CLOSED", "IMMEDIATE"));
        when(clubDao.findClubMemberRelation(1L, 7L)).thenReturn(null);

        ClubDetailView view = clubDetailService.getDetail("1", 7L);

        assertThat(view.club().recruitStatus()).isEqualTo("CLOSED");
        assertThat(view.actionLabel()).isEqualTo("현재 모집하지 않습니다.");
    }

    @Test
    void invalidOrUnavailableClubUsesSameSafeNotFoundException() {
        assertThatThrownBy(() -> clubDetailService.getDetail("abc", null))
                .isInstanceOf(ClubNotFoundException.class)
                .hasMessage("존재하지 않거나 이용할 수 없는 모임입니다.");
        assertThatThrownBy(() -> clubDetailService.getDetail("0", null))
                .isInstanceOf(ClubNotFoundException.class);

        when(clubDao.findPublicClubDetail(99L)).thenReturn(null);
        assertThatThrownBy(() -> clubDetailService.getDetail("99", 7L))
                .isInstanceOf(ClubNotFoundException.class);
    }

    private ClubDetail detail(String recruitStatus, String joinType) {
        return new ClubDetail(
                1L,
                null,
                "주말 독서 모임",
                "독서",
                "서울 마포구",
                "함께 책을 읽어요.",
                "매주 한 권을 정해 이야기하는 모임입니다.",
                "MIXED",
                20,
                60,
                3,
                20,
                recruitStatus,
                joinType,
                "읽고 싶은 책을 알려주세요.",
                2L,
                "벼리",
                null,
                LocalDateTime.of(2026, 7, 20, 10, 0),
                LocalDateTime.of(2026, 7, 25, 10, 0),
                LocalDateTime.of(2026, 7, 26, 12, 0));
    }
}
