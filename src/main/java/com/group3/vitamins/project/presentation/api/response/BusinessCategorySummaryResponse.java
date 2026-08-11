package com.group3.vitamins.project.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사업 카테고리 요약")
public record BusinessCategorySummaryResponse(

        @Schema(description = "카테고리 ID", example = "1")
        Long categoryId,

        @Schema(description = "카테고리 이름", example = "환경")
        String name,

        @Schema(description = "업무코드. 없으면 null", example = "ENV", nullable = true)
        String code,

        @Schema(description = "이 카테고리가 삭제됐는지 (배지 표시용). "
                + "true 면 이름은 그대로 보이지만 새로 선택할 수는 없다", example = "false")
        boolean deleted
) {
}