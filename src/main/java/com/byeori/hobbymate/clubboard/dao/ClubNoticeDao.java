package com.byeori.hobbymate.clubboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.vo.ClubBoardAccess;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeAdjacentPost;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeDetail;
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

    ClubNoticeDetail findNoticeDetail(
            @Param("clubId") Long clubId,
            @Param("postId") Long postId);

    int incrementNoticeViewCount(
            @Param("clubId") Long clubId,
            @Param("postId") Long postId);

    ClubNoticeAdjacentPost findPreviousNotice(
            @Param("clubId") Long clubId,
            @Param("postId") Long postId,
            @Param("createdAt") java.time.LocalDateTime createdAt);

    ClubNoticeAdjacentPost findNextNotice(
            @Param("clubId") Long clubId,
            @Param("postId") Long postId,
            @Param("createdAt") java.time.LocalDateTime createdAt);

    int countPinnedPosts(
            @Param("clubId") Long clubId,
            @Param("postType") String postType);

    int insertClubPost(ClubPostCreation post);
}
