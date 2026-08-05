package com.group3.vitamins.project.block.presentation.api.request;

import com.group3.vitamins.project.block.application.command.UpdateBlockLayoutCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "블록 배치 변경 요청 — 드래그 결과를 한 번에 보낸다")
public record BlockLayoutUpdateRequest(

        @Schema(description = "배치 목록. 비어 있으면 400", requiredMode = Schema.RequiredMode.REQUIRED)
        List<LayoutItem> layouts
) {

    /** 요청을 커맨드로 옮긴다. 값 검증은 서비스가 한다 (@NotNull 은 명세 에러코드를 못 낸다). */
    public UpdateBlockLayoutCommand toCommand(Long stepId, String requesterUserId, String role) {
        List<UpdateBlockLayoutCommand.BlockLayout> converted = layouts == null
                ? null
                : layouts.stream().map(LayoutItem::toLayout).toList();

        return new UpdateBlockLayoutCommand(stepId, converted, requesterUserId, role);
    }

    @Schema(description = "블록 한 개의 배치")
    public record LayoutItem(

            @Schema(description = "블록 ID", example = "15",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Long blockId,

            @Schema(description = "행 인덱스 (0부터)", example = "0",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Integer rowIndex,

            @Schema(description = "행 내 좌→우 순서", example = "0",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Integer sortOrder,

            @Schema(description = "열 병합 수 (1~3). 총 열 수는 3 고정", example = "2",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Integer colSpan
    ) {

        UpdateBlockLayoutCommand.BlockLayout toLayout() {
            return new UpdateBlockLayoutCommand.BlockLayout(blockId, rowIndex, sortOrder, colSpan);
        }
    }
}