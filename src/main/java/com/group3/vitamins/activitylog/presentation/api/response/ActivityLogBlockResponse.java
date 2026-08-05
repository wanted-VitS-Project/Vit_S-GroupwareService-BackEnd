package com.group3.vitamins.activitylog.presentation.api.response;

import com.group3.vitamins.activitylog.application.result.ActivityLogResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ActivityLogBlockResponse(

        @Schema(description = "Block ID", example = "15")
        Long blockId,

        @Schema(description = "Block 제목", example = "제안서 작성 체크리스트")
        String title,

        @Schema(description = "Block 유형", example = "CHECKLIST")
        String type
) {

    public static ActivityLogBlockResponse from(ActivityLogResult.Block block) {
        return new ActivityLogBlockResponse(
                block.blockId(),
                block.title(),
                block.type()
        );
    }
}