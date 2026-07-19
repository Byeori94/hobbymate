package com.byeori.hobbymate.member.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.member.vo.MemberAuthInfo;
import com.byeori.hobbymate.member.vo.MemberMyPageInfo;
import com.byeori.hobbymate.member.vo.MemberProfileUpdate;
import com.byeori.hobbymate.member.vo.MemberRegistration;

@Mapper
public interface MemberDao {
    MemberAuthInfo findAuthByLoginId(@Param("loginId") String loginId);
    String findActivePasswordHashByMemberId(@Param("memberId") Long memberId);
    MemberMyPageInfo findActiveMemberForMyPage(@Param("memberId") Long memberId);
    boolean existsNicknameExceptMember(
            @Param("nickname") String nickname,
            @Param("memberId") Long memberId);
    boolean existsByLoginId(@Param("loginId") String loginId);
    boolean existsByNickname(@Param("nickname") String nickname);
    boolean existsByEmail(@Param("email") String email);
    int insertMember(MemberRegistration member);
    int updateActiveMemberProfile(MemberProfileUpdate member);
    int updateActiveMemberPassword(
            @Param("memberId") Long memberId,
            @Param("encodedPassword") String encodedPassword);
}
