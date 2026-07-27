package com.byeori.hobbymate.clubboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;
import com.byeori.hobbymate.clubboard.vo.ClubPostCreation;

@Mapper
public interface ClubNoticeDao {

    ClubBoardAccess findClubBoardAccess(
            @Param("clubId") Long clubId,
            @Param("memberId") Long memberId);

    Long lockClubForPostCreation(@Param("clubId") Long clubId);

    long countNotices(ClubNoticeSearchCondition search);

    List<ClubNoticeListItem> findNotices(ClubNoticeSearchCondition search);

    int countPinnedPosts(
            @Param("clubId") Long clubId,
            @Param("postType") String postType);

    int insertClubPost(ClubPostCreation post);
}
