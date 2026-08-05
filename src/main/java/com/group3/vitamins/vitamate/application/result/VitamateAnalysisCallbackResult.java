package com.group3.vitamins.vitamate.application.result;

// Python callback 처리 결과
public record VitamateAnalysisCallbackResult(
        boolean accepted,
        Long analysisId,
        String analysisStatus,
        String reason
) {
}