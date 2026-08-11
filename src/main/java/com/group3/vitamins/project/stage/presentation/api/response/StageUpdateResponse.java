package com.group3.vitamins.project.stage.presentation.api.response;

import com.group3.vitamins.project.stage.application.result.StageResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스테이지 수정 응답")
public record StageUpdateResponse(

        @Schema(description = "스테이지 ID", example = "7")
        Long stageId,

        @Schema(description = "스테이지명", example = "제안·계약")
        String name,

        @Schema(description = "정렬 순서", example = "1")
        int sortOrder,

        @Schema(description = "저장 후의 새 version. 화면 상태를 이 값으로 교체해야 "
                + "다음 저장이 409 가 되지 않는다", example = "8")
        int version
) {

    public static StageUpdateResponse from(StageResult result) {
        return new StageUpdateResponse(
                result.stageId(), result.name(), result.sortOrder(), result.version());
    }
}
