package com.group3.vitamins.vitamate.analysis.presentation.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 비타메이트 분석 요청 API의 요청 본문
@Schema(description = "비타메이트 분석 요청")
public record CreateVitamateAnalysisRequest(

        @Schema(description = "분석할 파일 버전 ID 목록", example = "[101, 102]")
        @NotEmpty(message = "분석할 문서를 선택해 주세요.")
        List<@NotNull(message = "파일 버전 ID는 null일 수 없습니다.") Long> fileVersionIds,

        @Schema(description = "분석 요청 프롬프트", example = "핵심 기술 요구사항과 위험 요소를 정리해줘.")
        @NotBlank(message = "프롬프트를 입력해 주세요.")
        String prompt
) {
}
