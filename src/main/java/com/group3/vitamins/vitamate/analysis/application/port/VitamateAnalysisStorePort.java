package com.group3.vitamins.vitamate.analysis.application.port;

import com.group3.vitamins.vitamate.analysis.application.result.CreateVitamateAnalysisResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 비타메이트 분석 요청 저장과 멱등성 조회를 담당하는 포트
public interface VitamateAnalysisStorePort {

    // 멱등성 키 기준으로 이미 생성된 분석 요청을 찾는다.
    Optional<ExistingAnalysis> findExistingAnalysis(Long vitamateBlockId, String requestedBy, String idempotencyKey);

    // 새 분석 요청을 PENDING 상태로 저장한다.
    CreateVitamateAnalysisResult savePendingAnalysis(NewAnalysis analysis);

    // 분석 요청과 선택된 파일 버전들을 연결해 저장한다.
    void saveAnalysisDocuments(Long analysisId, List<NewAnalysisDocument> documents);

    // 분석 요청 당시 선택된 검토 템플릿 스냅샷을 저장한다.
    void saveAnalysisTemplates(Long analysisId, List<NewAnalysisTemplate> templates);

    boolean markProcessing(Long analysisId, String attemptId, LocalDateTime startedAt, LocalDateTime leaseExpiresAt);

    boolean markCompleted(Long analysisId, String attemptId, String result, LocalDateTime completedAt);

    boolean markFailedFromProcessing(Long analysisId, String attemptId, String errorMessage, LocalDateTime failedAt);

    boolean markFailedFromPending(Long analysisId, String errorMessage, LocalDateTime failedAt);

    Optional<String> findAnalysisStatus(Long analysisId);

    boolean existsAllCitationTargets(Long analysisId, List<NewCitation> citations);

    void saveAnalysisCitations(Long analysisId, List<NewCitation> citations);

    record NewCitation(
            Long documentChunkId,
            Long fileVersionId,
            Integer rankOrder,
            java.math.BigDecimal distanceScore,
            String excerpt
    ) {
    }

    record NewAnalysis(
            Long vitamateBlockId,
            String requestedBy,
            String idempotencyKey,
            String requestHash,
            String prompt,
            String reviewType,
            String reviewCategoryCodes,
            LocalDateTime requestedAt
    ) {
    }

    record NewAnalysisTemplate(
            String reviewType,
            String categoryCode,
            String categoryName,
            String promptTemplate,
            String templateVersion,
            Integer sortOrder
    ) {
    }

    record NewAnalysisDocument(
            Long fileVersionId,
            String documentRole
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
