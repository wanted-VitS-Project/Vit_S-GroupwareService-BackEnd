package com.group3.vitamins.activitylog.application.service;

import com.group3.vitamins.activitylog.application.query.ActivityLogListQuery;
import com.group3.vitamins.activitylog.application.port.ActivityLogQueryPort;
import com.group3.vitamins.activitylog.application.result.ActivityLogLookupResult;
import com.group3.vitamins.activitylog.application.result.ActivityLogPageResult;
import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import com.group3.vitamins.activitylog.application.usecase.ActivityLogQueryUseCase;
import com.group3.vitamins.activitylog.domain.exception.ActivityLogErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogQueryService implements ActivityLogQueryUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final Set<String> GLOBAL_ACCESS_ROLES = Set.of("ADMIN", "MASTER");

    private final ActivityLogQueryPort activityLogQueryPort;

    @Override
    public ActivityLogPageResult getActivityLogs(ActivityLogListQuery query) {
        int size = resolveSize(query.size());
        validateCursor(query.cursor());

        var step = activityLogQueryPort.findStepAccess(query.stepId(), query.requesterUserId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        assertProjectAccess(query.role(), step.permission());
        validateBlockFilter(query.stepId(), query.blockId());

        List<ActivityLogLookupResult> rows = activityLogQueryPort.findActivityLogs(
                query.stepId(), query.blockId(), query.cursor(), size + 1);

        boolean hasNext = rows.size() > size;
        List<ActivityLogLookupResult> pageRows = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext && !pageRows.isEmpty()
                ? pageRows.get(pageRows.size() - 1).activityLogId()
                : null;

        return new ActivityLogPageResult(
                pageRows.stream().map(ActivityLogResult::from).toList(),
                nextCursor,
                hasNext
        );
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size <= 0) {
            throw new ValidationException(ActivityLogErrorCode.ACTIVITY_LOG_SIZE_INVALID);
        }
        return size;
    }

    private void validateCursor(Long cursor) {
        if (cursor != null && cursor <= 0) {
            throw new ValidationException(ActivityLogErrorCode.ACTIVITY_LOG_CURSOR_INVALID);
        }
    }

    private void assertProjectAccess(String role, String permission) {
        if (GLOBAL_ACCESS_ROLES.contains(role)) {
            return;
        }
        if (permission == null || "NONE".equals(permission)) {
            throw new ForbiddenException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    private void validateBlockFilter(Long stepId, Long blockId) {
        if (blockId == null) {
            return;
        }

        var block = activityLogQueryPort.findBlockStep(blockId)
                .orElseThrow(() -> new NotFoundException(ActivityLogErrorCode.BLOCK_NOT_FOUND));

        if (!stepId.equals(block.stepId())) {
            throw new ValidationException(ActivityLogErrorCode.ACTIVITY_LOG_BLOCK_STEP_MISMATCH);
        }
    }
}
