package com.group3.vitamins.vitamate.analysis.presentation.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 비타메이트 분석 요청 API의 요청 본문입니다.
@Schema(description = "비타메이트 분석 요청")
public record CreateVitamateAnalysisRequest(

        @Schema(description = "분석할 파일 버전 ID 목록", example = "[101, 102]")
        @NotEmpty(message = "분석할 문서를 선택해 주세요.")
        List<@NotNull(message = "파일 버전 ID는 null일 수 없습니다.") Long> fileVersionIds,

        @Schema(description = "검토 유형", example = "COST_REPORT")
        @NotBlank(message = "검토 유형을 선택해 주세요.")
        String reviewType,

        @Schema(description = "검토 카테고리 코드 목록", example = "[\"COST_RESULT\", \"COST_STATEMENT\"]")
        @NotEmpty(message = "검토 카테고리를 하나 이상 선택해 주세요.")
        List<@NotBlank(message = "검토 카테고리 코드는 비어 있을 수 없습니다.") String> reviewCategoryCodes,

        @Schema(description = "템플릿에 덧붙일 사용자 추가 요청", example = "금액과 부가세 포함 여부를 특히 확인해줘.")
        String additionalInstruction
) {
}
