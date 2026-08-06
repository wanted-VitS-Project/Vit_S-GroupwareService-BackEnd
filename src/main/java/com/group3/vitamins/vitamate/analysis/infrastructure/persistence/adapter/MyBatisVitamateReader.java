package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateBlockReaderPort;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateFileReaderPort;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.mapper.VitamateAnalysisMapper;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.row.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 비타메이트 블록 권한과 파일 버전 검증 조회를 담당하는 MyBatis 어댑터
@Component
@RequiredArgsConstructor
public class MyBatisVitamateReader implements VitamateBlockReaderPort, VitamateFileReaderPort, VitamateAnalysisReaderPort {

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

    // Python worker가 처리할 수 있는 PROCESSING 분석 작업 입력을 조회한다.
    @Override
    public Optional<VitamateAnalysisJobDetail> findProcessingAnalysisJob(Long analysisId, String attemptId) {
        return Optional.ofNullable(mapper.findProcessingAnalysisJob(analysisId, attemptId))
                .map(job -> toAnalysisJobDetail(
                        job,
                        mapper.findAnalysisJobDocuments(analysisId),
                        mapper.findAnalysisJobChunks(analysisId)
                ));
    }

    // 비타메이트 블록에 속한 분석 실행 이력 목록을 조회합니다.
    @Override
    public List<VitamateAnalysisHistory> findBlockAnalysisHistories(Long vitamateBlockId) {
        return mapper.findBlockAnalysisHistories(vitamateBlockId).stream()
                .map(this::toAnalysisHistory)
                .toList();
    }

    // MyBatis Row를 application reader port의 이력 값으로 변환합니다.
    private VitamateAnalysisHistory toAnalysisHistory(VitamateAnalysisHistoryRow row) {
        return new VitamateAnalysisHistory(
                row.getAnalysisId(),
                row.getPrompt(),
                row.getAnalysisStatus(),
                row.getCreatedAt(),
                row.getCompletedAt()
        );
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

    // 분석 작업 기본 정보, 선택 문서, 후보 청크를 Python worker 입력 값으로 조립한다.
    private VitamateAnalysisJobDetail toAnalysisJobDetail(
            VitamateAnalysisJobRow job,
            List<VitamateAnalysisJobDocumentRow> documents,
            List<VitamateAnalysisJobChunkRow> chunks
    ) {
        Map<Long, List<JobChunk>> chunksByFileVersionId = groupChunksByFileVersionId(chunks);
        List<Long> fileVersionIds = documents.stream()
                .map(VitamateAnalysisJobDocumentRow::getFileVersionId)
                .toList();

        return new VitamateAnalysisJobDetail(
                job.getAnalysisId(),
                job.getAttemptId(),
                job.getPrompt(),
                new JobSearchScope(
                        job.getProjectId(),
                        job.getBlockId(),
                        fileVersionIds
                ),
                documents.stream()
                        .map(document -> toJobDocument(document, chunksByFileVersionId))
                        .toList()
        );
    }

    // 후보 청크를 파일 버전 ID별로 묶어 문서 응답에 붙일 수 있게 준비한다.
    private Map<Long, List<JobChunk>> groupChunksByFileVersionId(List<VitamateAnalysisJobChunkRow> chunks) {
        Map<Long, List<JobChunk>> chunksByFileVersionId = new LinkedHashMap<>();

        for (VitamateAnalysisJobChunkRow chunk : chunks) {
            chunksByFileVersionId
                    .computeIfAbsent(chunk.getFileVersionId(), ignored -> new ArrayList<>())
                    .add(toJobChunk(chunk));
        }

        return chunksByFileVersionId;
    }

    // 문서 Row와 해당 문서의 청크 목록을 Python worker 문서 값으로 변환한다.
    private JobDocument toJobDocument(
            VitamateAnalysisJobDocumentRow row,
            Map<Long, List<JobChunk>> chunksByFileVersionId
    ) {
        return new JobDocument(
                row.getFileVersionId(),
                row.getFileName(),
                chunksByFileVersionId.getOrDefault(row.getFileVersionId(), List.of())
        );
    }

    // 청크 Row를 Python worker 후보 청크 값으로 변환한다.
    private JobChunk toJobChunk(VitamateAnalysisJobChunkRow row) {
        return new JobChunk(
                row.getDocumentChunkId(),
                row.getChromaId(),
                row.getPageNumber(),
                row.getExcerpt()
        );
    }
}
