package com.group3.vitamins.vitamate.application.port;

import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 비타메이트 분석 요청 저장과 멱등성 조회를 담당하는 포트
public interface VitamateAnalysisStore {

    // 멱등성 키 기준으로 이미 생성된 분석 요청을 찾는다.
    Optional<ExistingAnalysis> findExistingAnalysis(Long vitamateBlockId, String requestedBy, String idempotencyKey);

    // 새 분석 요청을 PENDING 상태로 저장한다.
    CreateVitamateAnalysisResult savePendingAnalysis(NewAnalysis analysis);

    // 분석 요청과 선택된 파일 버전들을 연결해 저장한다.
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
