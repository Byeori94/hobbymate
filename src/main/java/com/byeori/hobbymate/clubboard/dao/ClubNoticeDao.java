package com.byeori.hobbymate.clubboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;

@Mapper
public interface ClubNoticeDao {

    ClubBoardAccess findClubBoardAccess(
            @Param("clubId") Long clubId,
            @Param("memberId") Long memberId);

    long countNotices(ClubNoticeSearchCondition search);

    List<ClubNoticeListItem> findNotices(ClubNoticeSearchCondition search);
}
