package com.group3.vitamins.project.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 스텝 애그리게이트 소관 테이블({@code step}) 직접 조회.
 * 진척률은 프로젝트의 속성이라(PRJ-013) 집계는 프로젝트 쪽에서 한다 — 명세도 진척률 조회를 Project 도메인으로 분류한다.
 */
@Mapper
public interface StepStatQueryMapper {

    @Select("""
            SELECT COUNT(*) AS totalCount,
                   COALESCE(SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END), 0) AS doneCount
              FROM step
             WHERE project_id = #{projectId}
               AND deleted_at IS NULL
            """)
    StepStatRow countByProjectId(@Param("projectId") Long projectId);
}