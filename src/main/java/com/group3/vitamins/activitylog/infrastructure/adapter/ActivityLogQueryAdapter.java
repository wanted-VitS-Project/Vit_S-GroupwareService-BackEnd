package com.group3.vitamins.activitylog.infrastructure.adapter;

import com.group3.vitamins.activitylog.application.port.ActivityLogQueryPort;
import com.group3.vitamins.activitylog.application.result.ActivityLogLookupResult;
import com.group3.vitamins.activitylog.application.result.BlockStepResult;
import com.group3.vitamins.activitylog.application.result.StepAccessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ActivityLogQueryAdapter implements ActivityLogQueryPort {

    private final ActivityLogQueryMapper activityLogQueryMapper;

    @Override
    public Optional<StepAccessResult> findStepAccess(Long stepId, String userId, Long companyId) {
        return activityLogQueryMapper.findStepAccess(stepId, userId, companyId)
                .map(row -> new StepAccessResult(row.stepId(), row.projectId(), row.permission()));
    }

    @Override
    public Optional<BlockStepResult> findBlockStep(Long blockId, Long companyId) {
        return activityLogQueryMapper.findBlockStep(blockId, companyId)
                .map(row -> new BlockStepResult(row.blockId(), row.stepId()));
    }

    @Override
    public List<ActivityLogLookupResult> findActivityLogs(
            Long stepId,
            Long blockId,
            Long cursor,
            int limit,
            Long companyId
    ) {
        return activityLogQueryMapper.findActivityLogs(stepId, blockId, cursor, limit, companyId).stream()
                .map(this::toResult)
                .toList();
    }

    private ActivityLogLookupResult toResult(ActivityLogRow row) {
        return new ActivityLogLookupResult(
                row.activityLogId(),
                row.action(),
                row.resourceId(),
                row.resourceName(),
                row.fieldName(),
                row.beforeValue(),
                row.afterValue(),
                row.actorUserId(),
                row.actorName(),
                row.actorResignedAt(),
                row.blockId(),
                row.blockTitle(),
                row.blockType(),
                row.createdAt()
        );
    }
}
