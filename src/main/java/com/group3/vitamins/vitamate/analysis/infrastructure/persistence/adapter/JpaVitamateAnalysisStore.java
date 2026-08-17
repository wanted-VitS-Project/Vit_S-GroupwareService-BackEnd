package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.analysis.domain.model.AnalysisStatus;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.DocumentChunkEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisCitationEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisDocumentEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisTemplateEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.DocumentChunkJpaRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateAnalysisCitationJpaRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateAnalysisDocumentJpaRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateAnalysisJpaRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateAnalysisTemplateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

// 비타메이트 분석 요청과 선택 문서를 JPA로 저장하는 어댑터
@Component
@RequiredArgsConstructor
public class JpaVitamateAnalysisStore implements VitamateAnalysisStorePort {

    private final VitamateAnalysisJpaRepository analysisRepository;
    private final VitamateAnalysisDocumentJpaRepository documentRepository;
    private final VitamateAnalysisTemplateJpaRepository templateRepository;
    private final VitamateAnalysisCitationJpaRepository citationRepository;
    private final DocumentChunkJpaRepository chunkRepository;

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
                analysis.reviewType(),
                analysis.reviewCategoryCodes(),
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
    public void saveAnalysisDocuments(Long analysisId, List<NewAnalysisDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        List<VitamateAnalysisDocumentEntity> documentEntities = documents.stream()
                .map(document -> VitamateAnalysisDocumentEntity.of(
                        analysisId,
                        document.fileVersionId(),
                        document.documentRole()
                ))
                .toList();

        documentRepository.saveAll(documentEntities);
    }

    // 분석 요청에서 선택한 템플릿을 요청 당시 값으로 고정해 저장한다.
    @Override
    public void saveAnalysisTemplates(Long analysisId, List<NewAnalysisTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<VitamateAnalysisTemplateEntity> templateEntities = templates.stream()
                .map(template -> VitamateAnalysisTemplateEntity.of(
                        analysisId,
                        template.reviewType(),
                        template.categoryCode(),
                        template.categoryName(),
                        template.promptTemplate(),
                        template.templateVersion(),
                        template.sortOrder(),
                        now
                ))
                .toList();

        templateRepository.saveAll(templateEntities);
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

    // 분석 요청의 현재 상태를 조회한다.
    @Override
    public Optional<String> findAnalysisStatus(Long analysisId) {
        return analysisRepository.findById(analysisId)
                .map(analysis -> analysis.getAnalysisStatus().name());
    }

    // callback citation이 요청 당시 선택한 문서와 그 문서의 청크만 참조하는지 확인한다.
    @Override
    public boolean existsAllCitationTargets(Long analysisId, List<NewCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return true;
        }

        Map<Long, VitamateAnalysisDocumentEntity> documentMap = findAnalysisDocumentMap(analysisId, citations);
        if (documentMap.size() != countDistinctFileVersions(citations)) {
            return false;
        }

        Map<Long, Long> activeChunkFileVersionById = findActiveChunkFileVersionsByChunkId(citations);

        return citations.stream()
                .allMatch(citation -> documentMap.containsKey(citation.fileVersionId())
                        && citation.fileVersionId().equals(
                                activeChunkFileVersionById.get(citation.documentChunkId())
                        ));
    }

    // citation이 가리키는 청크들을 한 번에 조회해 청크ID → 소속 파일버전ID 맵으로 만든다.
    private Map<Long, Long> findActiveChunkFileVersionsByChunkId(List<NewCitation> citations) {
        List<Long> chunkIds = citations.stream()
                .map(NewCitation::documentChunkId)
                .distinct()
                .toList();

        return chunkRepository.findAllByIdInAndDeletedAtIsNull(chunkIds)
                .stream()
                .collect(Collectors.toMap(
                        DocumentChunkEntity::getId,
                        DocumentChunkEntity::getFileVersionId
                ));
    }

    // 검증이 끝난 분석 citation 목록을 저장한다.
    @Override
    public void saveAnalysisCitations(Long analysisId, List<NewCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return;
        }

        Map<Long, VitamateAnalysisDocumentEntity> documentMap = findAnalysisDocumentMap(analysisId, citations);
        List<VitamateAnalysisCitationEntity> citationEntities = citations.stream()
                .map(citation -> VitamateAnalysisCitationEntity.of(
                        analysisId,
                        documentMap.get(citation.fileVersionId()).getId(),
                        citation.documentChunkId(),
                        citation.rankOrder(),
                        citation.distanceScore(),
                        citation.excerpt()
                ))
                .toList();

        citationRepository.saveAll(citationEntities);
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

    // citation의 fileVersionId로 분석 대상 문서 엔티티를 찾는다.
    private Map<Long, VitamateAnalysisDocumentEntity> findAnalysisDocumentMap(
            Long analysisId,
            List<NewCitation> citations
    ) {
        List<Long> fileVersionIds = citations.stream()
                .map(NewCitation::fileVersionId)
                .distinct()
                .toList();

        return documentRepository.findByAnalysisIdAndFileVersionIdInAndDeletedAtIsNull(analysisId, fileVersionIds)
                .stream()
                .collect(Collectors.toMap(
                        VitamateAnalysisDocumentEntity::getFileVersionId,
                        Function.identity()
                ));
    }

    // citation 목록 안에 있는 서로 다른 파일 버전 개수를 센다.
    private long countDistinctFileVersions(List<NewCitation> citations) {
        return citations.stream()
                .map(NewCitation::fileVersionId)
                .distinct()
                .count();
    }
}
