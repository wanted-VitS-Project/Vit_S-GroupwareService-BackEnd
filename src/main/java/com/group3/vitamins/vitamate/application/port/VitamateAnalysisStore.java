package com.group3.vitamins.vitamate.application.port;

import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 비타메이트 분석 요청 저장과 멱등성 조회를 담당하는 포트
public interface VitamateAnalysisStore {

    Optional<ExistingAnalysis> findExistingAnalysis(Long vitamateBlockId, String requestedBy, String idempotencyKey);

    CreateVitamateAnalysisResult savePendingAnalysis(NewAnalysis analysis);

    void saveAnalysisDocuments(Long analysisId, List<Long> fileVersionIds);

    record NewAnalysis(
            Long vitamateBlockId,
            String requestedBy,
            String idempotencyKey,
            String requestHash,
            String prompt,
            LocalDateTime requestedAt
    ) {
    }

    record ExistingAnalysis(
            Long analysisId,
            String requestHash,
            String analysisStatus,
            LocalDateTime requestedAt
    ) {
    }
}
