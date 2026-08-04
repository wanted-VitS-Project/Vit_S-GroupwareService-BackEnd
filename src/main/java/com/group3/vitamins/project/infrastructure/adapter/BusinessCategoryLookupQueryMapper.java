package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 카테고리 도메인 소관 테이블({@code business_category}) 직접 조회.
 * 남의 테이블에 JPA 엔티티를 만들지 않는다 (BCT 의 {@code ProjectCategoryLinkQueryMapper} 선례).
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/project/BusinessCategoryLookupQueryMapper.xml}.
 */
@Mapper
public interface BusinessCategoryLookupQueryMapper {

    List<BusinessCategoryRow> findByIds(@Param("categoryIds") List<Long> categoryIds);
}