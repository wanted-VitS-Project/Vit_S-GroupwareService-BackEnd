package com.group3.vitamins.vitamate.analysis.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 비타메이트 분석 상세와 worker 작업 조회를 위한 읽기 포트입니다.
public interface VitamateAnalysisReaderPort {

    // 요청자가 접근 가능한 분석이면 분석 본문, 선택 문서, 근거 목록을 함께 반환합니다.
    Optional<VitamateAnalysisDetail> findAccessibleAnalysis(Long analysisId, String userId);

    // Python worker가 처리할 PROCESSING 분석 작업 입력을 조회합니다.
    Optional<VitamateAnalysisJobDetail> findProcessingAnalysisJob(Long analysisId, String attemptId);

    // 비타메이트 블록에 속한 분석 실행 이력 목록을 지정한 개수까지 조회합니다.
    List<VitamateAnalysisHistory> findBlockAnalysisHistories(Long vitamateBlockId, int limit);

    record VitamateAnalysisDetail(
            Long analysisId,
            Long blockId,
            String reviewType,
            List<String> reviewCategoryCodes,
            String prompt,
            List<TemplateVersion> templateVersions,
            String analysisStatus,
            String result,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            List<Document> documents,
            List<Citation> citations
    ) {
    }

    record TemplateVersion(
            String categoryCode,
            String templateVersion
    ) {
    }

    record Document(
            Long fileVersionId,
            String fileName,
            String documentRole
    ) {
    }

    record Citation(
            Integer rankOrder,
            Long fileVersionId,
            Long documentChunkId,
            Integer pageNumber,
            String excerpt
    ) {
    }

    record VitamateAnalysisJobDetail(
            Long analysisId,
            String attemptId,
            String reviewType,
            List<String> reviewCategoryCodes,
            String prompt,
            List<JobReviewTemplate> reviewTemplates,
            JobSearchScope searchScope,
            List<JobDocument> documents
    ) {
    }

    record JobReviewTemplate(
            String reviewType,
            String categoryCode,
            String categoryName,
            String promptTemplate,
            String templateVersion
    ) {
    }

    record JobSearchScope(
            Long projectId,
            Long blockId,
            List<Long> fileVersionIds
    ) {
    }

    record JobDocument(
            Long fileVersionId,
            String fileName,
            String documentRole,
            List<JobChunk> chunks
    ) {
    }

    record JobChunk(
            Long documentChunkId,
            String chromaId,
            Integer pageNumber,
            String excerpt
    ) {
    }

    record VitamateAnalysisHistory(
            Long analysisId,
            String reviewType,
            List<String> reviewCategoryCodes,
            String prompt,
            String analysisStatus,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
    }
}
