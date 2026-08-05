package com.group3.vitamins.vitamate.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.application.result.VitamateAnalysisCallbackResult;

// Python callback 처리 결과를 반환하는 내부 응답 DTO
public record VitamateAnalysisCallbackResponse(
        boolean accepted,
        Long analysisId,
        String analysisStatus,
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
