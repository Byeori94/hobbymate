package com.byeori.hobbymate.club.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.dto.ClubDetailView;
import com.byeori.hobbymate.club.vo.ClubDetail;
import com.byeori.hobbymate.club.vo.ClubMemberRelation;
import com.byeori.hobbymate.common.exception.ClubNotFoundException;

@Service
public class ClubDetailService {

    private final ClubDao clubDao;

    public ClubDetailService(ClubDao clubDao) {
        this.clubDao = clubDao;
    }

    @Transactional(readOnly = true)
    public ClubDetailView getDetail(String rawClubId, Long memberId) {
        Long clubId = parseClubId(rawClubId);
        ClubDetail club = clubDao.findPublicClubDetail(clubId);
        if (club == null) {
            throw new ClubNotFoundException();
        }

        if (memberId == null) {
            return actionView(club, "ANONYMOUS", "", "로그인", "로그인 후 가입할 수 있습니다.");
        }

        ClubMemberRelation relation = clubDao.findClubMemberRelation(clubId, memberId);
        return relationshipView(club, relation);
    }

    private Long parseClubId(String rawClubId) {
        try {
            long clubId = Long.parseLong(rawClubId);
            if (clubId < 1) {
                throw new ClubNotFoundException();
            }
            return clubId;
        } catch (NumberFormatException ex) {
            throw new ClubNotFoundException();
        }
    }

    private ClubDetailView relationshipView(
            ClubDetail club,
            ClubMemberRelation relation) {
        if (relation == null) {
            return recruitmentView(club);
        }
        if (relation.isPermanentlyBanned()) {
            return actionView(
                    club,
                    "BANNED",
                    "가입 제한",
                    "가입할 수 없음",
                    "이 모임에 다시 가입할 수 없습니다.");
        }
        if (relation.isActiveMember()) {
            return activeMemberView(club, relation.memberRole());
        }
        if (relation.isPendingJoin()) {
            return actionView(
                    club,
                    "PENDING",
                    "승인 대기",
                    "가입 승인 대기 중",
                    "모임장 또는 운영진의 승인을 기다리고 있습니다.");
        }
        if (relation.rejoinAvailableAt() != null
                && relation.rejoinAvailableAt().isAfter(LocalDateTime.now())) {
            return actionView(
                    club,
                    "REJOIN_RESTRICTED",
                    "재가입 제한",
                    "지금은 가입할 수 없음",
                    "탈퇴 또는 강퇴 후 일정 시간 동안 다시 가입할 수 없습니다.");
        }
        return recruitmentView(club);
    }

    private ClubDetailView activeMemberView(ClubDetail club, String role) {
        return switch (role == null ? "" : role) {
            case "LEADER" -> actionView(
                    club,
                    "LEADER",
                    "모임장",
                    "모임 정보 수정",
                    "내가 운영 중인 모임입니다.");
            case "MANAGER" -> actionView(
                    club,
                    "MANAGER",
                    "운영진",
                    "모임 관리",
                    "운영진으로 활동 중인 모임입니다.");
            default -> actionView(
                    club,
                    "MEMBER",
                    "가입 회원",
                    "모임 활동 보기",
                    "가입한 모임입니다.");
        };
    }

    private ClubDetailView recruitmentView(ClubDetail club) {
        return switch (club.recruitStatus() == null ? "" : club.recruitStatus()) {
            case "OPEN" -> actionView(
                    club,
                    "NON_MEMBER",
                    "",
                    "APPROVAL".equals(club.joinType()) ? "가입 신청" : "가입하기",
                    "가입 기능은 준비 중입니다.");
            case "FULL" -> actionView(
                    club,
                    "NON_MEMBER",
                    "",
                    "정원이 마감되었습니다.",
                    "현재 정원이 모두 찼습니다.");
            default -> actionView(
                    club,
                    "NON_MEMBER",
                    "",
                    "현재 모집하지 않습니다.",
                    "모집이 다시 시작되면 가입할 수 있습니다.");
        };
    }

    private ClubDetailView actionView(
            ClubDetail club,
            String relationshipType,
            String relationshipLabel,
            String actionLabel,
            String actionMessage) {
        return new ClubDetailView(
                club,
                relationshipType,
                relationshipLabel,
                actionLabel,
                actionMessage);
    }
}
