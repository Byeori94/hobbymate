package com.byeori.hobbymate.member.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.member.vo.MemberAuthInfo;
import com.byeori.hobbymate.member.vo.MemberRegistration;

@Mapper
public interface MemberDao {
    MemberAuthInfo findAuthByLoginId(@Param("loginId") String loginId);
    boolean existsByLoginId(@Param("loginId") String loginId);
    boolean existsByNickname(@Param("nickname") String nickname);
    boolean existsByEmail(@Param("email") String email);
    int insertMember(MemberRegistration member);
}
