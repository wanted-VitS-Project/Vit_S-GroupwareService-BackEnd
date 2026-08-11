package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ActivityLogResourceResponse(

        @Schema(description = "Block 내부 데이터 ID. Block 자체 활동이면 null", example = "41")
        Long resourceId,

        @Schema(description = "Block 내부 데이터 표시명 스냅샷. 없으면 null", example = "제안서 작성")
        String name
) {

    public static ActivityLogResourceResponse from(ActivityLogResult.Resource resource) {
        return new ActivityLogResourceResponse(
                resource.resourceId(),
                resource.name()
        );
    }
}
