package com.group3.vitamins.project.stage.presentation.api.response;

import com.group3.vitamins.project.stage.application.result.StageSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스테이지 목록 조회 응답")
public record StageListResponse(

        @Schema(description = "스테이지 목록 (sortOrder 오름차순)")
        List<StageItemResponse> stages
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static StageListResponse from(List<StageSummary> summaries) {
        return new StageListResponse(summaries.stream()
                .map(s -> new StageItemResponse(
                        s.stageId(), s.name(), s.sortOrder(), s.stepCount(), s.version()))
                .toList());
    }

    @Schema(description = "스테이지 요약. 스테이지는 권한·상태를 저장하지 않는다 (STG-004 · INV-01)")
    public record StageItemResponse(
            @Schema(description = "스테이지 ID", example = "7") Long stageId,
            @Schema(description = "스테이지명", example = "제안") String name,
            @Schema(description = "정렬 순서", example = "1") int sortOrder,
            @Schema(description = "소속 스텝 수. 요청자가 볼 수 없는 스텝은 세지 않는다", example = "3")
            int stepCount,
            @Schema(description = "낙관적 락 버전. 수정·순서 변경 요청에 그대로 실어 보낸다", example = "7")
            int version
    ) {
    }
}