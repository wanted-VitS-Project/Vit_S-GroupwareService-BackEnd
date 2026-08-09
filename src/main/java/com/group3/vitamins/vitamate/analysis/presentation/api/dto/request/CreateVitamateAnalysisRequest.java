package com.group3.vitamins.vitamate.analysis.presentation.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 비타메이트 문서 비교 분석 요청 본문입니다.
@Schema(description = "비타메이트 문서 비교 분석 요청")
public record CreateVitamateAnalysisRequest(
        @Schema(description = "비교 기준 파일 버전 ID 목록", example = "[101, 102]")
        @NotEmpty(message = "비교 기준 문서를 선택해 주세요.")
        List<@NotNull(message = "기준 문서 버전 ID는 null일 수 없습니다.") Long> referenceFileVersionIds,

        @Schema(description = "검토 대상 파일 버전 ID 목록", example = "[201, 202]")
        @NotEmpty(message = "검토 대상 문서를 선택해 주세요.")
        List<@NotNull(message = "검토 대상 문서 버전 ID는 null일 수 없습니다.") Long> targetFileVersionIds,

        @Schema(description = "검토 유형", example = "COST_REPORT")
        @NotBlank(message = "검토 유형을 선택해 주세요.")
        String reviewType,

        @Schema(description = "검토 카테고리 코드 목록", example = "[\"COST_RESULT\", \"COST_STATEMENT\"]")
        @NotEmpty(message = "검토 카테고리를 하나 이상 선택해 주세요.")
        List<@NotBlank(message = "검토 카테고리 코드는 비어 있을 수 없습니다.") String> reviewCategoryCodes,

        @Schema(description = "사용자가 확정한 최종 검토 프롬프트", example = "기준 문서와 다른 항목을 출처와 함께 정리해줘.")
        @NotBlank(message = "검토 프롬프트를 입력해 주세요.")
        String prompt
) {
}
