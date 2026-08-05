package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogPageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ActivityLogListResponse(

        @Schema(description = "활동 기록 목록")
        List<ActivityLogItemResponse> activities,

        @Schema(description = "다음 조회 커서, 없으면 null", example = "500")
        Long nextCursor,

        @Schema(description = "다음 기록 존재 여부", example = "true")
        boolean hasNext
) {

    public static ActivityLogListResponse from(ActivityLogPageResult result) {
        return new ActivityLogListResponse(
                result.activities().stream()
                        .map(ActivityLogItemResponse::from)
                        .toList(),
                result.nextCursor(),
                result.hasNext()
        );
    }
}
