package com.group3.vitamins.vitamate.analysis.application.command;

import java.util.List;

// 비교 기준 문서와 검토 대상 문서를 포함한 분석 생성 명령입니다.
public record CreateVitamateAnalysisCommand(
        Long blockId,
        String requestedBy,
        String role,
        String idempotencyKey,
        List<Long> referenceFileVersionIds,
        List<Long> targetFileVersionIds,
        String reviewType,
        List<String> reviewCategoryCodes,
        String prompt
) {
}
