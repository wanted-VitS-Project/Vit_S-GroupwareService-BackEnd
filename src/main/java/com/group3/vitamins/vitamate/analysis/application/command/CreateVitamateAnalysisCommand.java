package com.group3.vitamins.vitamate.analysis.application.command;

import java.util.List;

// 비타메이트 분석 요청 생성 서비스에 전달하는 명령 객체
public record CreateVitamateAnalysisCommand(
        Long blockId,
        String requestedBy,
        String idempotencyKey,
        List<Long> fileVersionIds,
        String prompt
) {
}