package com.group3.vitamins.project.stage.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 스텝 애그리게이트 소관 테이블({@code step} · {@code step_permission}) 직접 조회.
 * step_permission 행이 없으면 프로젝트 권한을 상속하므로 NULL 은 제외하지 않는다.
 */
@Mapper
public interface StepCountQueryMapper {

    @Select("""
            SELECT s.stage_id AS stageId,
                   COUNT(*)   AS stepCount
              FROM step s
              LEFT JOIN step_permission sp
                     ON sp.step_id = s.step_id
                    AND sp.user_id = #{userId}
             WHERE s.project_id = #{projectId}
               AND s.stage_id IS NOT NULL
               AND s.deleted_at IS NULL
               AND (sp.permission IS NULL OR sp.permission <> 'NONE')
             GROUP BY s.stage_id
            """)
    List<StepCountRow> countByStage(@Param("projectId") Long projectId,
                                    @Param("userId") String userId);
}