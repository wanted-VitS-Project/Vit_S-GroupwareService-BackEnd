package com.group3.vitamins.businesscategory.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 프로젝트 도메인 소관 테이블({@code project_business_category}) 직접 조회.
 * 남의 테이블에 JPA 엔티티를 만들면 소유가 흐려지므로 MyBatis 로 읽는다 ({@code AuthQueryMapper} 선례).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/businesscategory/ProjectCategoryLinkQueryMapper.xml}.
 */
@Mapper
public interface ProjectCategoryLinkQueryMapper {

    /** 프로젝트에 연결된 카테고리 ID 전체. 반환 행 수는 카테고리 개수를 넘지 않는다. */
    List<Long> findLinkedCategoryIds();

    /** 특정 카테고리에 연결된 프로젝트 수. */
    long countLinkedProjects(@Param("categoryId") Long categoryId);
}