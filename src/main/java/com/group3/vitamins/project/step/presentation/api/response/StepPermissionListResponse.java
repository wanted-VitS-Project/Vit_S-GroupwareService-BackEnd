package com.group3.vitamins.project.step.presentation.api.response;

import com.group3.vitamins.project.step.application.result.StepPermissionSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스텝 권한 목록 응답")
public record StepPermissionListResponse(

        @Schema(description = "참여자별 판정 결과")
        List<Item> permissions
) {

    @Schema(description = "참여자 한 명의 스텝 권한")
    public record Item(

            @Schema(description = "사원 사번", example = "E2024007")
            String userId,

            @Schema(description = "이름", example = "김동훈")
            String name,

            @Schema(description = "최종 판정 등급", example = "NONE")
            String permission,

            @Schema(description = "step_permission 행 보유 여부. false 면 프로젝트 권한 상속",
                    example = "true")
            boolean overridden
    ) {
    }

    public static StepPermissionListResponse from(List<StepPermissionSummary> summaries) {
        return new StepPermissionListResponse(summaries.stream()
                .map(summary -> new Item(summary.userId(), summary.name(),
                        summary.permission(), summary.overridden()))
                .toList());
    }
}
