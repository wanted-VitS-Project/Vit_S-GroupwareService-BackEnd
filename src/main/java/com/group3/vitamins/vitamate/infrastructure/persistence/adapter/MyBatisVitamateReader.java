package com.group3.vitamins.vitamate.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisReader;
import com.group3.vitamins.vitamate.application.port.VitamateBlockReader;
import com.group3.vitamins.vitamate.application.port.VitamateFileReader;
import com.group3.vitamins.vitamate.infrastructure.persistence.mapper.VitamateAnalysisMapper;
import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateAnalysisCitationRow;
import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateAnalysisDocumentRow;
import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateAnalysisRow;
import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateBlockContextRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

// 비타메이트 블록 권한과 파일 버전 검증 조회를 담당하는 MyBatis 어댑터
@Component
@RequiredArgsConstructor
public class MyBatisVitamateReader implements VitamateBlockReader, VitamateFileReader, VitamateAnalysisReader {

    private final VitamateAnalysisMapper mapper;

    // 요청자가 접근할 수 있는 AI 블록인지 확인하고 프로젝트 컨텍스트를 조회한다.
    @Override
    public Optional<VitamateBlockContext> findAccessibleVitamateBlock(Long blockId, String userId) {
        return Optional.ofNullable(mapper.findAccessibleVitamateBlock(blockId, userId))
                .map(this::toContext);
    }

    // 요청한 파일 버전 ID가 모두 같은 프로젝트의 완료된 파일 버전인지 확인한다.
    @Override
    public boolean existsAllCompletedFileVersionsInProject(Long projectId, List<Long> fileVersionIds) {
        if (fileVersionIds == null || fileVersionIds.isEmpty()) {
            return false;
        }

        int distinctRequestCount = new HashSet<>(fileVersionIds).size();
        int matchedCount = mapper.countCompletedFileVersionsInProject(projectId, fileVersionIds);
        return matchedCount == distinctRequestCount;
    }

    // 요청자가 접근할 수 있는 분석 상세를 문서와 citation까지 함께 조회한다.
    @Override
    public Optional<VitamateAnalysisDetail> findAccessibleAnalysis(Long analysisId, String userId) {
        return Optional.ofNullable(mapper.findAccessibleAnalysis(analysisId, userId))
                .map(analysis -> toAnalysisDetail(
                        analysis,
                        mapper.findAnalysisDocuments(analysisId),
                        mapper.findAnalysisCitations(analysisId)
                ));
    }

    // MyBatis Row 객체를 application 포트의 컨텍스트 값으로 변환한다.
    private VitamateBlockContext toContext(VitamateBlockContextRow row) {
        return new VitamateBlockContext(
                row.getBlockId(),
                row.getVitamateBlockId(),
                row.getStepId(),
                row.getProjectId()
        );
    }

    // 분석 본문 Row와 하위 Row들을 application 포트 결과로 조립한다.
    private VitamateAnalysisDetail toAnalysisDetail(
            VitamateAnalysisRow analysis,
            List<VitamateAnalysisDocumentRow> documents,
            List<VitamateAnalysisCitationRow> citations
    ) {
        return new VitamateAnalysisDetail(
                analysis.getAnalysisId(),
                analysis.getBlockId(),
                analysis.getPrompt(),
                analysis.getAnalysisStatus(),
                analysis.getResult(),
                analysis.getErrorMessage(),
                analysis.getCreatedAt(),
                analysis.getCompletedAt(),
                documents.stream()
                        .map(this::toDocument)
                        .toList(),
                citations.stream()
                        .map(this::toCitation)
                        .toList()
        );
    }

    // 문서 Row를 포트 문서 값으로 변환한다.
    private Document toDocument(VitamateAnalysisDocumentRow row) {
        return new Document(
                row.getFileVersionId(),
                row.getFileName()
        );
    }

    // citation Row를 포트 citation 값으로 변환한다.
    private Citation toCitation(VitamateAnalysisCitationRow row) {
        return new Citation(
                row.getRankOrder(),
                row.getFileVersionId(),
                row.getDocumentChunkId(),
                row.getPageNumber(),
                row.getExcerpt()
        );
    }
}
