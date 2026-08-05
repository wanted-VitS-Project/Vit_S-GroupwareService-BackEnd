package com.group3.vitamins.project.stage.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 스텝 애그리게이트 소관 테이블({@code step} · {@code step_permission}) 직접 조회.
 * step_permission 행이 없으면 프로젝트 권한을 상속하므로 NULL 은 제외하지 않는다.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/stage/StepCountQueryMapper.xml}.
 */
@Mapper
public interface StepCountQueryMapper {

    List<StepCountRow> countByStage(@Param("projectId") Long projectId,
                                    @Param("userId") String userId);
}