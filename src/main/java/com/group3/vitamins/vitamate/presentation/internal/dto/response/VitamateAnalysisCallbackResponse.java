package com.group3.vitamins.vitamate.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.application.result.VitamateAnalysisCallbackResult;
import io.swagger.v3.oas.annotations.media.Schema;

// Python callback 처리 결과를 반환하는 내부 응답 DTO
@Schema(description = "Python 분석 결과 callback 응답")
public record VitamateAnalysisCallbackResponse(
        @Schema(description = "결과 저장 여부", example = "true")
        boolean accepted,

        @Schema(description = "분석 ID", example = "501")
        Long analysisId,

        @Schema(description = "저장된 상태. 저장하지 않은 경우 현재 상태", example = "COMPLETED")
        String analysisStatus,

        @Schema(description = "accepted=false일 때 무시 사유", example = "attempt_mismatch_or_already_finished")
        String reason
) {

    // application result를 내부 API 응답으로 변환한다.
    public static VitamateAnalysisCallbackResponse from(VitamateAnalysisCallbackResult result) {
        return new VitamateAnalysisCallbackResponse(
                result.accepted(),
                result.analysisId(),
                result.analysisStatus(),
                result.reason()
        );
    }
}
