package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepOrderResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스텝 순서 변경 응답")
public record StepOrderResponse(

        @Schema(description = "재정렬 결과. 요청한 순서 그대로 돌려준다")
        List<Item> steps
) {

    @Schema(description = "서버가 확정한 스텝 위치")
    public record Item(

            @Schema(description = "스텝 ID", example = "11")
            Long stepId,

            @Schema(description = "소속 스테이지 ID. 미소속이면 null", example = "7", nullable = true)
            Long stageId,

            @Schema(description = "정렬 순서", example = "1")
            int sortOrder
    ) {
    }

    public static StepOrderResponse from(List<StepOrderResult> results) {
        return new StepOrderResponse(results.stream()
                .map(result -> new Item(result.stepId(), result.stageId(), result.sortOrder()))
                .toList());
    }
}