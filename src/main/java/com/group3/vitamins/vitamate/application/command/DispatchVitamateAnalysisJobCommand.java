package com.group3.vitamins.vitamate.application.command;

// 비타메이트 분석 작업 큐 발행 요청 값
public record DispatchVitamateAnalysisJobCommand(
        Long analysisId
) {
}
