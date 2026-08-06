package com.group3.vitamins.vitamate.analysis.application.command;

import java.math.BigDecimal;
import java.util.List;

// Python worker가 전달한 분석 결과 callback command
public record HandleVitamateAnalysisCallbackCommand(
        Long analysisId,
        String attemptId,
        String analysisStatus,
        String result,
        List<Citation> citations,
        String errorMessage
) {
    public record Citation(
            Long documentChunkId,
            Long fileVersionId,
            Integer rankOrder,
            BigDecimal distanceScore,
            String excerpt
    ) {
    }
}