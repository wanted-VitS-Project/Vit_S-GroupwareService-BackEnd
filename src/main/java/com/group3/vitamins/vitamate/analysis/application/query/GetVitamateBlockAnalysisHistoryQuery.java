package com.group3.vitamins.vitamate.analysis.application.query;

// 특정 비타메이트 블록의 분석 실행 이력을 조회하기 위한 Query입니다.
public record GetVitamateBlockAnalysisHistoryQuery(
        Long blockId,
        String userId
) {
}