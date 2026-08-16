package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.AdminTreeStepProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 전사 파일 트리 — 스텝 목록(§14.3). */
@Schema(description = "전사 파일 트리 스텝 목록(§14.3)")
public record AdminTreeStepListResponse(
        @Schema(description = "스텝 목록") List<Step> steps
) {

    @Schema(description = "스텝 노드")
    public record Step(
            @Schema(description = "스텝 번호") Long stepId,
            @Schema(description = "스텝명") String name,
            @Schema(description = "정렬 순서") int sortOrder,
            @Schema(description = "스텝 상태") String status
    ) {
        static Step from(AdminTreeStepProjection p) {
            return new Step(p.stepId(), p.name(), p.sortOrder(), p.status());
        }
    }

    public static AdminTreeStepListResponse from(List<AdminTreeStepProjection> list) {
        return new AdminTreeStepListResponse(list.stream().map(Step::from).toList());
    }
}
