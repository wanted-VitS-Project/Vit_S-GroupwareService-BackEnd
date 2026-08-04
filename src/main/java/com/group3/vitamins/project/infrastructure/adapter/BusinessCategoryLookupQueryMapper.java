package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 카테고리 도메인 소관 테이블({@code business_category}) 직접 조회.
 * 남의 테이블에 JPA 엔티티를 만들지 않는다 (BCT 의 {@code ProjectCategoryLinkQueryMapper} 선례).
 */
@Mapper
public interface BusinessCategoryLookupQueryMapper {

    @Select("""
            <script>
            SELECT business_category_id AS categoryId, name AS name
              FROM business_category
             WHERE deleted_at IS NULL
               AND business_category_id IN
                   <foreach collection="categoryIds" item="id" open="(" separator="," close=")">
                       #{id}
                   </foreach>
            </script>
            """)
    List<BusinessCategoryRow> findByIds(@Param("categoryIds") List<Long> categoryIds);
}