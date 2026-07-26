package com.byeori.hobbymate.admin.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.admin.dto.VerificationSearchCondition;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationRow;
import com.byeori.hobbymate.admin.vo.AdminMemberVerificationState;
import com.byeori.hobbymate.admin.vo.AdminVerificationHistory;

@Mapper
public interface AdminMemberVerificationDao {

    boolean isAdminMember(@Param("memberId") Long memberId);

    long countAuthTargets(@Param("search") VerificationSearchCondition search);

    List<AdminMemberVerificationRow> findAuthTargets(
            @Param("search") VerificationSearchCondition search,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countCancelTargets(@Param("search") VerificationSearchCondition search);

    List<AdminMemberVerificationRow> findCancelTargets(
            @Param("search") VerificationSearchCondition search,
            @Param("offset") int offset,
            @Param("limit") int limit);

    AdminMemberVerificationState findMemberState(@Param("memberId") Long memberId);

    AdminMemberVerificationState findMemberStateForUpdate(@Param("memberId") Long memberId);

    int updateTemporaryVerification(
            @Param("memberId") Long memberId,
            @Param("name") String name,
            @Param("birthDate") LocalDate birthDate,
            @Param("gender") String gender,
            @Param("phone") String phone,
            @Param("ciHash") String ciHash,
            @Param("reason") String reason,
            @Param("adminMemberId") Long adminMemberId);

    int cancelTemporaryVerification(
            @Param("memberId") Long memberId,
            @Param("adminMemberId") Long adminMemberId);

    int insertVerificationHistory(AdminVerificationHistory history);
}
