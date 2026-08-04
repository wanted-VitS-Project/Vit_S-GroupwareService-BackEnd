package com.group3.vitamins.project.step.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 이슈 애그리게이트 소관 테이블({@code issue}) 직접 조회 — 스텝 진척률 집계용.
 * 이슈가 0건인 스텝은 결과 행이 없다.
 */
@Mapper
public interface IssueStatQueryMapper {

    @Select("""
            <script>
            SELECT step_id AS stepId,
                   COUNT(*) AS totalCount,
                   COALESCE(SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END), 0) AS doneCount,
                   COALESCE(SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0)
                       AS inProgressCount
              FROM issue
             WHERE step_id IN
            <foreach item="id" collection="stepIds" open="(" separator="," close=")">#{id}</foreach>
               AND deleted_at IS NULL
             GROUP BY step_id
            </script>
            """)
    List<IssueStatRow> countByStepIds(@Param("stepIds") Collection<Long> stepIds);
}
