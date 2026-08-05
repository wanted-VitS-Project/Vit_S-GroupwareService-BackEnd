package com.group3.vitamins.vitamate.application.query;

// 비타메이트 분석 상세 조회 조건
public record GetVitamateAnalysisQuery(
        Long analysisId,
        String userId
) {
}