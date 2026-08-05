package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ActivityLogItemResponse(

        @Schema(description = "활동 기록 ID", example = "501")
        Long activityLogId,

        @Schema(description = "CREATE, MODIFY, DELETE", example = "MODIFY")
        String action,

        @Schema(description = "BLOCK 또는 RESOURCE", example = "RESOURCE")
        String targetType,

        @Schema(description = "FE 단순 표시용 이름", example = "제안서 작성")
        String displayName,

        @Schema(description = "수정 필드, 해당하지 않으면 null", example = "completed")
        String fieldName,

        @Schema(description = "변경 전 값", example = "false")
        String beforeValue,

        @Schema(description = "변경 후 값", example = "true")
        String afterValue,

        @Schema(description = "Block 내부 데이터. Block 자체 활동이면 resourceId/name 모두 null")
        ActivityLogResourceResponse resource,

        @Schema(description = "활동 수행자")
        ActivityLogActorResponse actor,

        @Schema(description = "활동이 발생한 Block")
        ActivityLogBlockResponse block,

        @Schema(description = "활동 발생 일시", example = "2026-08-02T14:32:00")
        LocalDateTime createdAt
) {

    public static ActivityLogItemResponse from(ActivityLogResult result) {
        return new ActivityLogItemResponse(
                result.activityLogId(),
                result.action(),
                result.targetType(),
                result.displayName(),
                result.fieldName(),
                result.beforeValue(),
                result.afterValue(),
                ActivityLogResourceResponse.from(result.resource()),
                ActivityLogActorResponse.from(result.actor()),
                ActivityLogBlockResponse.from(result.block()),
                result.createdAt()
        );
    }
}
