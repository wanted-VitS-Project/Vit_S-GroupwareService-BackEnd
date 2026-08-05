package com.group3.vitamins.vitamate.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStore;
import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisDocumentEntity;
import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisEntity;
import com.group3.vitamins.vitamate.infrastructure.persistence.repository.VitamateAnalysisDocumentJpaRepository;
import com.group3.vitamins.vitamate.infrastructure.persistence.repository.VitamateAnalysisJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.group3.vitamins.vitamate.domain.model.AnalysisStatus;

import java.time.LocalDateTime;


import java.util.List;
import java.util.Optional;

// 비타메이트 분석 요청과 선택 문서를 JPA로 저장하는 어댑터
@Component
@RequiredArgsConstructor
public class JpaVitamateAnalysisStore implements VitamateAnalysisStore {

    private final VitamateAnalysisJpaRepository analysisRepository;
    private final VitamateAnalysisDocumentJpaRepository documentRepository;

    // 같은 멱등성 키로 이미 생성된 분석 요청이 있는지 조회한다.
    @Override
    public Optional<ExistingAnalysis> findExistingAnalysis(Long vitamateBlockId, String requestedBy, String idempotencyKey) {
        return analysisRepository.findByVitamateBlockIdAndRequestedByAndIdempotencyKey(
                        vitamateBlockId,
                        requestedBy,
                        idempotencyKey
                )
                .map(this::toExistingAnalysis);
    }

    // 새 분석 요청을 PENDING 상태로 저장하고 unique 충돌을 즉시 감지할 수 있게 flush한다.
    @Override
    public CreateVitamateAnalysisResult savePendingAnalysis(NewAnalysis analysis) {
        VitamateAnalysisEntity savedAnalysis = analysisRepository.saveAndFlush(VitamateAnalysisEntity.pending(
                analysis.vitamateBlockId(),
                analysis.requestedBy(),
                analysis.idempotencyKey(),
                analysis.requestHash(),
                analysis.prompt(),
                analysis.requestedAt()
        ));

        return new CreateVitamateAnalysisResult(
                savedAnalysis.getId(),
                savedAnalysis.getAnalysisStatus().name(),
                savedAnalysis.getCreatedAt()
        );
    }

    // 분석 요청에 선택된 파일 버전 목록을 연결 테이블에 저장한다.
    @Override
    public void saveAnalysisDocuments(Long analysisId, List<Long> fileVersionIds) {
        if (fileVersionIds == null || fileVersionIds.isEmpty()) {
            return;
        }

        List<VitamateAnalysisDocumentEntity> documents = fileVersionIds.stream()
                .map(fileVersionId -> VitamateAnalysisDocumentEntity.of(analysisId, fileVersionId))
                .toList();

        documentRepository.saveAll(documents);
    }

    // PENDING 상태의 분석 요청을 PROCESSING 상태로 변경한다.
    @Override
    public boolean markProcessing(
            Long analysisId,
            String attemptId,
            LocalDateTime startedAt,
            LocalDateTime leaseExpiresAt
    ) {
        int updatedCount = analysisRepository.markProcessing(
                analysisId,
                AnalysisStatus.PENDING,
                AnalysisStatus.PROCESSING,
                attemptId,
                startedAt,
                leaseExpiresAt
        );

        return updatedCount == 1;
    }

    // PROCESSING 상태의 분석 요청을 COMPLETED 상태로 변경하고 결과를 저장한다.
    @Override
    public boolean markCompleted(
            Long analysisId,
            String attemptId,
            String result,
            LocalDateTime completedAt
    ) {
        int updatedCount = analysisRepository.markCompleted(
                analysisId,
                AnalysisStatus.PROCESSING,
                AnalysisStatus.COMPLETED,
                attemptId,
                result,
                completedAt
        );

        return updatedCount == 1;
    }

    // PROCESSING 상태의 분석 요청을 FAILED 상태로 변경하고 실패 사유를 저장한다.
    @Override
    public boolean markFailedFromProcessing(
            Long analysisId,
            String attemptId,
            String errorMessage,
            LocalDateTime failedAt
    ) {
        int updatedCount = analysisRepository.markFailedFromProcessing(
                analysisId,
                AnalysisStatus.PROCESSING,
                AnalysisStatus.FAILED,
                attemptId,
                errorMessage,
                failedAt
        );

        return updatedCount == 1;
    }

    // 아직 처리되지 않은 PENDING 상태의 분석 요청을 FAILED 상태로 마감한다.
    @Override
    public boolean markFailedFromPending(
            Long analysisId,
            String errorMessage,
            LocalDateTime failedAt
    ) {
        int updatedCount = analysisRepository.markFailedFromPending(
                analysisId,
                AnalysisStatus.PENDING,
                AnalysisStatus.FAILED,
                errorMessage,
                failedAt
        );

        return updatedCount == 1;
    }

    // JPA 엔티티를 application 계층에서 쓰는 기존 분석 요청 값으로 변환한다.
    private ExistingAnalysis toExistingAnalysis(VitamateAnalysisEntity analysis) {
        return new ExistingAnalysis(
                analysis.getId(),
                analysis.getRequestHash(),
                analysis.getAnalysisStatus().name(),
                analysis.getCreatedAt()
        );
    }
}
