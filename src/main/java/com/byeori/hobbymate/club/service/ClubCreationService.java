package com.byeori.hobbymate.club.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.dto.ClubCreateRequest;
import com.byeori.hobbymate.club.dto.ClubCreationPage;
import com.byeori.hobbymate.club.vo.ClubCreation;
import com.byeori.hobbymate.club.vo.ClubCreationMember;
import com.byeori.hobbymate.club.vo.ClubLeaderRegistration;
import com.byeori.hobbymate.common.exception.ClubCreationException;
import com.byeori.hobbymate.common.exception.ClubImageException;
import com.byeori.hobbymate.file.storage.ClubImageStorage;

@Service
public class ClubCreationService {

    public static final int MAX_LED_CLUBS = 5;

    private static final Logger log = LoggerFactory.getLogger(ClubCreationService.class);
    private static final Set<String> GENDER_POLICIES = Set.of("MIXED", "FEMALE", "MALE");
    private static final Set<String> JOIN_TYPES = Set.of("IMMEDIATE", "APPROVAL");

    private final ClubDao clubDao;
    private final ClubImageStorage clubImageStorage;

    public ClubCreationService(ClubDao clubDao, ClubImageStorage clubImageStorage) {
        this.clubDao = clubDao;
        this.clubImageStorage = clubImageStorage;
    }

    @Transactional(readOnly = true)
    public ClubCreationPage prepareCreation(Long memberId) {
        ClubCreationMember member = clubDao.findMemberForCreation(requireMemberId(memberId));
        validateEligibility(member);
        validateClubLimit(memberId);
        return new ClubCreationPage(clubDao.findActiveCategories());
    }

    @Transactional
    public Long createClub(
            Long memberId,
            ClubCreateRequest request,
            MultipartFile representativeImage) {
        Long authenticatedMemberId = requireMemberId(memberId);
        ClubCreationMember member = clubDao.findMemberForCreationForUpdate(authenticatedMemberId);
        validateEligibility(member);
        validateClubLimit(authenticatedMemberId);
        normalizeAndValidate(request);

        if (!clubDao.existsActiveCategory(request.getCategoryId())) {
            throw new ClubCreationException("categoryId", "현재 사용할 수 없는 카테고리입니다.");
        }

        String storedFileName = storeOptionalImage(representativeImage);
        if (storedFileName != null) {
            deleteImageOnRollback(storedFileName, authenticatedMemberId);
        }

        ClubCreation club = new ClubCreation(
                authenticatedMemberId,
                request.getCategoryId(),
                request.getClubName(),
                request.getClubSummary(),
                request.getClubDescription(),
                storedFileName,
                request.getActivityRegion(),
                request.getGenderPolicy(),
                request.getMinAge(),
                request.getMaxAge(),
                request.getMaxMemberCount(),
                request.getJoinType(),
                "APPROVAL".equals(request.getJoinType()) ? blankToNull(request.getJoinGuide()) : null);

        try {
            if (clubDao.insertClub(club) != 1 || club.getClubId() == null) {
                throw new ClubCreationException(null, "모임을 개설할 수 없습니다.");
            }
            if (clubDao.insertLeader(
                    new ClubLeaderRegistration(club.getClubId(), authenticatedMemberId)) != 1) {
                throw new ClubCreationException(null, "모임장을 등록할 수 없습니다.");
            }
            return club.getClubId();
        } catch (RuntimeException ex) {
            if (storedFileName != null
                    && !TransactionSynchronizationManager.isSynchronizationActive()) {
                deleteCompensationImage(storedFileName, authenticatedMemberId);
            }
            if (ex instanceof ClubCreationException) {
                log.warn("모임 개설 업무 검증 또는 등록 실패: memberId={}", authenticatedMemberId);
                throw ex;
            }
            log.error("모임 개설 DB 처리 실패: memberId={}", authenticatedMemberId, ex);
            throw new ClubCreationException(null, "모임 개설 중 오류가 발생했습니다.");
        }
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new ClubCreationException(
                    null, "로그인 후 이용할 수 있습니다.", true);
        }
        return memberId;
    }

    private void validateEligibility(ClubCreationMember member) {
        if (member == null) {
            throw new ClubCreationException(
                    null, "회원정보를 확인할 수 없습니다.", true);
        }
        if (!"ACTIVE".equals(member.memberStatus())) {
            throw new ClubCreationException(
                    null, "현재 계정 상태에서는 모임을 개설할 수 없습니다.", true);
        }
        if (!"Y".equals(member.identityVerifiedYn())) {
            throw new ClubCreationException(
                    null, "본인인증을 완료한 회원만 모임을 개설할 수 있습니다.", true);
        }
    }

    private void validateClubLimit(Long memberId) {
        if (clubDao.countLedActiveOrBlockedClubs(memberId) >= MAX_LED_CLUBS) {
            throw new ClubCreationException(
                    null, "개설할 수 있는 모임 수는 최대 5개입니다.", true);
        }
    }

    private void normalizeAndValidate(ClubCreateRequest request) {
        request.setClubName(trim(request.getClubName()));
        request.setActivityRegion(trim(request.getActivityRegion()));
        request.setClubSummary(trim(request.getClubSummary()));
        request.setClubDescription(trim(request.getClubDescription()));
        request.setJoinGuide(trim(request.getJoinGuide()));

        if (request.getClubName() == null
                || request.getClubName().length() < 2
                || request.getClubName().length() > 50) {
            throw new ClubCreationException(
                    "clubName", "모임명은 2자 이상 50자 이하로 입력해 주세요.");
        }
        if (request.getActivityRegion() == null
                || request.getActivityRegion().length() > 200) {
            throw new ClubCreationException(
                    "activityRegion", "활동 지역을 입력해 주세요.");
        }
        if (request.getClubSummary() == null
                || request.getClubSummary().length() > 500) {
            throw new ClubCreationException(
                    "clubSummary", "한줄 소개를 500자 이하로 입력해 주세요.");
        }
        if (request.getClubDescription() == null
                || request.getClubDescription().length() < 10
                || request.getClubDescription().length() > 2000) {
            throw new ClubCreationException(
                    "clubDescription", "모임 소개는 10자 이상 2,000자 이하로 입력해 주세요.");
        }
        if (!GENDER_POLICIES.contains(request.getGenderPolicy())) {
            throw new ClubCreationException(
                    "genderPolicy", "올바른 성별 유형을 선택해 주세요.");
        }
        validateAges(request.getMinAge(), request.getMaxAge());
        if (request.getMaxMemberCount() == null
                || request.getMaxMemberCount() < 2
                || request.getMaxMemberCount() > 1000) {
            throw new ClubCreationException(
                    "maxMemberCount", "최대 정원은 2명 이상 1,000명 이하로 입력해 주세요.");
        }
        if (!JOIN_TYPES.contains(request.getJoinType())) {
            throw new ClubCreationException(
                    "joinType", "올바른 가입 방식을 선택해 주세요.");
        }
        if ("APPROVAL".equals(request.getJoinType())
                && request.getJoinGuide() != null
                && request.getJoinGuide().length() > 500) {
            throw new ClubCreationException(
                    "joinGuide", "가입 신청 안내는 500자 이하로 입력해 주세요.");
        }
    }

    private void validateAges(Integer minAge, Integer maxAge) {
        if (minAge == null || minAge < 18 || minAge > 100) {
            throw new ClubCreationException(
                    "minAge", "최소 연령은 만 18세 이상 100세 이하로 입력해 주세요.");
        }
        if (maxAge == null || maxAge < 18 || maxAge > 100) {
            throw new ClubCreationException(
                    "maxAge", "최대 연령은 만 18세 이상 100세 이하로 입력해 주세요.");
        }
        if (minAge > maxAge) {
            throw new ClubCreationException(
                    "maxAge", "최대 연령은 최소 연령보다 작을 수 없습니다.");
        }
    }

    private String storeOptionalImage(MultipartFile representativeImage) {
        if (representativeImage == null || representativeImage.isEmpty()) {
            return null;
        }
        return clubImageStorage.store(representativeImage);
    }

    private void deleteImageOnRollback(String storedFileName, Long memberId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteCompensationImage(storedFileName, memberId);
                }
            }
        });
    }

    private void deleteCompensationImage(String storedFileName, Long memberId) {
        try {
            clubImageStorage.delete(storedFileName);
        } catch (ClubImageException ex) {
            log.warn("모임 대표 이미지 보상 삭제 실패: memberId={}", memberId);
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
