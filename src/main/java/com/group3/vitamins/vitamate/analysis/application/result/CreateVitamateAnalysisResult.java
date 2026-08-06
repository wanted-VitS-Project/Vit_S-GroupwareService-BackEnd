package com.group3.vitamins.vitamate.analysis.application.result;

import java.time.LocalDateTime;

// 비타메이트 분석 요청 생성 결과
public record CreateVitamateAnalysisResult(
        Long analysisId,
        String analysisStatus,
        LocalDateTime requestedAt
) {
}