package com.group3.vitamins.vitamate.analysis.application.result;

import java.time.LocalDateTime;

// 비타메이트 분석 워커가 선점한 실행 정보를 담는 결과
public record StartVitamateAnalysisResult(
        Long analysisId,
        String attemptId,
        LocalDateTime startedAt,
        LocalDateTime leaseExpiresAt
) {
}