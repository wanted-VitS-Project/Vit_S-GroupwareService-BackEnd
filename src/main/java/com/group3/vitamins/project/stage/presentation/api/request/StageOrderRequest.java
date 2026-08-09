package com.group3.vitamins.project.stage.presentation.api.request;

import com.group3.vitamins.project.stage.application.command.ReorderStagesCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 스테이지 순서 변경 요청. 사이드바 <b>전체</b>의 최종 순서를 담아 보내는 계약이다.
 * ⛔ 하위 스텝은 건드리지 않는다 (STG-002).
 */
@Schema(description = "스테이지 순서 변경 요청 — 사이드바 전체의 최종 순서")
public record StageOrderRequest(

        @Valid
        @NotEmpty(message = "STAGE_ORDER_INVALID|순서를 변경할 스테이지가 없습니다.")
        @Schema(description = "재정렬 대상 목록")
        List<Item> orders
) {

    @Schema(description = "스테이지 하나의 새 순서")
    public record Item(

            @NotNull(message = "STAGE_ORDER_INVALID|스테이지 ID 가 없습니다.")
            @Schema(description = "스테이지 ID", example = "8")
            Long stageId,

            @NotNull(message = "STAGE_ORDER_INVALID|정렬 순서가 없습니다.")
            @Schema(description = "새 정렬 순서", example = "1")
            Integer sortOrder
    ) {
    }

    public ReorderStagesCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new ReorderStagesCommand(
                projectId,
                orders.stream()
                        .map(item -> new ReorderStagesCommand.Item(
                                item.stageId(), item.sortOrder()))
                        .toList(),
                requesterUserId, role);
    }
}
