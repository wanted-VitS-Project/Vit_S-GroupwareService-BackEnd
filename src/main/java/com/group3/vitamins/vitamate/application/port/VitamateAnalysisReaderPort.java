package com.group3.vitamins.vitamate.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 비타메이트 분석 상세 조회를 위한 읽기 포트입니다.
 */
public interface VitamateAnalysisReaderPort {

    // 요청자가 접근 가능한 분석이면 분석 본문, 선택 문서, 근거 목록을 함께 반환한다.
    Optional<VitamateAnalysisDetail> findAccessibleAnalysis(Long analysisId, String userId);

    // Python worker가 처리할 PROCESSING 분석 작업 입력을 조회한다.
    Optional<VitamateAnalysisJobDetail> findProcessingAnalysisJob(Long analysisId, String attemptId);

    record VitamateAnalysisDetail(
            Long analysisId,
            Long blockId,
            String prompt,
            String analysisStatus,
            String result,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            List<Document> documents,
            List<Citation> citations
    ) {
    }

    record Document(
            Long fileVersionId,
            String fileName
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
            String prompt,
            JobSearchScope searchScope,
            List<JobDocument> documents
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
}
