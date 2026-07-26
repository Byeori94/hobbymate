package com.byeori.hobbymate.club.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.club.vo.ClubCategory;
import com.byeori.hobbymate.club.vo.ClubCreation;
import com.byeori.hobbymate.club.vo.ClubCreationMember;
import com.byeori.hobbymate.club.vo.ClubLeaderRegistration;

@Mapper
public interface ClubDao {

    ClubCreationMember findMemberForCreation(@Param("memberId") Long memberId);

    ClubCreationMember findMemberForCreationForUpdate(@Param("memberId") Long memberId);

    int countLedActiveOrBlockedClubs(@Param("memberId") Long memberId);

    List<ClubCategory> findActiveCategories();

    boolean existsActiveCategory(@Param("categoryId") Long categoryId);

    int insertClub(ClubCreation club);

    int insertLeader(ClubLeaderRegistration leader);
}
