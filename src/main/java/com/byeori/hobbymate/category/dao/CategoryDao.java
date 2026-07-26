package com.byeori.hobbymate.category.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.byeori.hobbymate.category.dto.CategorySearchCondition;
import com.byeori.hobbymate.category.vo.AdminCategory;
import com.byeori.hobbymate.category.vo.CategoryCommand;

@Mapper
public interface CategoryDao {

    boolean isActiveAdmin(@Param("memberId") Long memberId);

    List<AdminCategory> findAdminCategories(CategorySearchCondition search);

    AdminCategory findById(@Param("categoryId") Long categoryId);

    AdminCategory findByIdForUpdate(@Param("categoryId") Long categoryId);

    boolean existsName(
            @Param("categoryName") String categoryName,
            @Param("excludedCategoryId") Long excludedCategoryId);

    int findNextDisplayOrder();

    int insertCategory(CategoryCommand command);

    int updateCategory(
            @Param("categoryId") Long categoryId,
            @Param("command") CategoryCommand command);

    int updateStatus(
            @Param("categoryId") Long categoryId,
            @Param("useYn") String useYn,
            @Param("adminMemberId") Long adminMemberId);

    List<Long> findAllIdsForUpdate();

    int updateDisplayOrder(
            @Param("categoryId") Long categoryId,
            @Param("displayOrder") int displayOrder,
            @Param("adminMemberId") Long adminMemberId);
}
