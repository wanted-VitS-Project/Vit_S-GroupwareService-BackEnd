package com.group3.vitamins.vitamate.presentation.api.dto.response;

import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 비타메이트 분석 요청 생성 API의 응답
@Schema(description = "비타메이트 분석 요청 생성 응답")
public record CreateVitamateAnalysisResponse(

        @Schema(description = "생성된 분석 ID", example = "501")
        Long analysisId,

        @Schema(description = "초기 분석 상태", example = "PENDING")
        String analysisStatus,

        @Schema(description = "요청 시각", example = "2026-08-04T14:05:00")
        LocalDateTime requestedAt
) {

    // application 결과 객체를 HTTP 응답 DTO로 변환한다.
    public static CreateVitamateAnalysisResponse from(CreateVitamateAnalysisResult result) {
        return new CreateVitamateAnalysisResponse(
                result.analysisId(),
                result.analysisStatus(),
                result.requestedAt()
        );
    }
}
