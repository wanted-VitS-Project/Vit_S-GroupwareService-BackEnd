package com.group3.vitamins.project.stage.presentation.api.response;

import com.group3.vitamins.project.stage.application.result.StageOrderResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스테이지 순서 변경 응답")
public record StageOrderResponse(

        @Schema(description = "재정렬 결과. 요청한 순서 그대로 돌려준다")
        List<Item> stages
) {

    @Schema(description = "서버가 확정한 스테이지 순서")
    public record Item(

            @Schema(description = "스테이지 ID", example = "8")
            Long stageId,

            @Schema(description = "정렬 순서", example = "1")
            int sortOrder
    ) {
    }

    public static StageOrderResponse from(List<StageOrderResult> results) {
        return new StageOrderResponse(results.stream()
                .map(result -> new Item(result.stageId(), result.sortOrder()))
                .toList());
    }
}
