package com.group3.vitamins.vitamate.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStore;
import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisDocumentEntity;
import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisEntity;
import com.group3.vitamins.vitamate.infrastructure.persistence.repository.VitamateAnalysisDocumentJpaRepository;
import com.group3.vitamins.vitamate.infrastructure.persistence.repository.VitamateAnalysisJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
