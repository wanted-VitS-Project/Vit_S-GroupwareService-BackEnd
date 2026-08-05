package com.group3.vitamins.activitylog.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ActivityLogQueryMapper {

    Optional<StepAccessRow> findStepAccess(
            @Param("stepId") Long stepId,
            @Param("userId") String userId
    );

    Optional<BlockStepRow> findBlockStep(@Param("blockId") Long blockId);

    List<ActivityLogRow> findActivityLogs(
            @Param("stepId") Long stepId,
            @Param("blockId") Long blockId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit
    );
}
