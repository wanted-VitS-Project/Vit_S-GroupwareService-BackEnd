package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.ReorderStepsCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 스텝 순서 변경 요청. 보드 <b>전체</b>의 최종 배치를 담아 보내는 계약이다 —
 * 바뀐 것만 보내면 목록 밖 스텝과 정렬 순서가 겹친다(DB 에 UNIQUE 제약이 없어 그대로 저장된다).
 */
@Schema(description = "스텝 순서 변경 요청 — 보드 전체의 최종 배치")
public record StepOrderRequest(

        @Valid
        @NotEmpty(message = "STEP_ORDER_INVALID|순서를 변경할 스텝이 없습니다.")
        @Schema(description = "재정렬 대상 목록")
        List<@NotNull(message = "STEP_ORDER_INVALID|순서 항목이 비어 있습니다.") Item> orders
) {

    @Schema(description = "스텝 하나의 새 위치")
    public record Item(

            @NotNull(message = "STEP_ORDER_INVALID|스텝 ID 가 없습니다.")
            @Schema(description = "스텝 ID", example = "11")
            Long stepId,

            @Schema(description = "이동할 스테이지 ID. 0 이거나 생략하면 미소속",
                    example = "7", nullable = true)
            Long stageId,

            @NotNull(message = "STEP_ORDER_INVALID|정렬 순서가 없습니다.")
            @Positive(message = "STEP_ORDER_INVALID|정렬 순서는 1 이상이어야 합니다.")
            @Schema(description = "새 정렬 순서", example = "1")
            Integer sortOrder,

            @NotNull(message = "STEP_VERSION_REQUIRED|버전 정보가 없습니다. 화면을 새로고침해 주세요.")
            @Schema(description = "이 스텝을 조회했을 때의 version. "
                    + "항목마다 따로 검사하며 하나라도 어긋나면 요청 전체가 409 다", example = "7")
            Integer version
    ) {

        /** 명세의 {@code 0 이면 미소속} 규약을 도메인이 쓰는 null 로 바꾼다. 생략도 같게 본다. */
        private Long resolvedStageId() {
            return stageId == null || stageId == 0L ? null : stageId;
        }
    }

    public ReorderStepsCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new ReorderStepsCommand(
                projectId,
                orders.stream()
                        .map(item -> new ReorderStepsCommand.Item(
                                item.stepId(), item.resolvedStageId(),
                                item.sortOrder(), item.version()))
                        .toList(),
                requesterUserId, role);
    }
}