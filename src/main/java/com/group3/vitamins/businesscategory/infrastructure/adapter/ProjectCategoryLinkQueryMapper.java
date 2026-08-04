package com.group3.vitamins.businesscategory.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 프로젝트 도메인 소관 테이블({@code project_business_category}) 직접 조회.
 * 남의 테이블에 JPA 엔티티를 만들면 소유가 흐려지므로 MyBatis 로 읽는다 ({@code AuthQueryMapper} 선례).
 */
@Mapper
public interface ProjectCategoryLinkQueryMapper {

    /** 프로젝트에 연결된 카테고리 ID 전체. 반환 행 수는 카테고리 개수를 넘지 않는다. */
    @Select("SELECT DISTINCT business_category_id FROM project_business_category")
    List<Long> findLinkedCategoryIds();

//    @Select("SELECT COUNT(*) FROM project_business_category WHERE business_category_id = #{categoryId}")
//    long countLinkedProjects(@Param("categoryId") Long categoryId);
}