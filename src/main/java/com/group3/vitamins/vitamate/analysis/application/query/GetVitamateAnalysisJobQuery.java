package com.group3.vitamins.vitamate.analysis.application.query;

// Python worker가 분석 작업 입력을 조회할 때 사용하는 query
public record GetVitamateAnalysisJobQuery(
        Long analysisId,
        String attemptId
) {
}