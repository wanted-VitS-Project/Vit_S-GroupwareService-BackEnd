package com.group3.vitamins.vitamate.analysis.application.result;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;

import java.util.List;

// Python worker에 전달할 비타메이트 분석 작업 입력 결과입니다.
public record VitamateAnalysisJobDetailResult(
        Long analysisId,
        String attemptId,
        String reviewType,
        List<String> reviewCategoryCodes,
        String prompt,
        List<ReviewTemplate> reviewTemplates,
        SearchScope searchScope,
        List<Document> documents
) {

    // 읽기 포트 결과를 내부 API 응답에 가까운 application result로 변환합니다.
    public static VitamateAnalysisJobDetailResult from(VitamateAnalysisReaderPort.VitamateAnalysisJobDetail detail) {
        return new VitamateAnalysisJobDetailResult(
                detail.analysisId(),
                detail.attemptId(),
                detail.reviewType(),
                detail.reviewCategoryCodes(),
                detail.prompt(),
                detail.reviewTemplates().stream()
                        .map(ReviewTemplate::from)
                        .toList(),
                SearchScope.from(detail.searchScope()),
                detail.documents().stream()
                        .map(Document::from)
                        .toList()
        );
    }

    public record ReviewTemplate(
            String reviewType,
            String categoryCode,
            String categoryName,
            String promptTemplate,
            String templateVersion
    ) {
        // 포트의 템플릿 스냅샷 값을 application result 값으로 변환합니다.
        private static ReviewTemplate from(VitamateAnalysisReaderPort.JobReviewTemplate template) {
            return new ReviewTemplate(
                    template.reviewType(),
                    template.categoryCode(),
                    template.categoryName(),
                    template.promptTemplate(),
                    template.templateVersion()
            );
        }
    }

    public record SearchScope(
            Long projectId,
            Long blockId,
            List<Long> fileVersionIds
    ) {
        // 포트의 검색 범위 값을 application result 검색 범위 값으로 변환합니다.
        private static SearchScope from(VitamateAnalysisReaderPort.JobSearchScope searchScope) {
            return new SearchScope(
                    searchScope.projectId(),
                    searchScope.blockId(),
                    searchScope.fileVersionIds()
            );
        }
    }

    public record Document(
            Long fileVersionId,
            String fileName,
            String documentRole,
            List<Chunk> chunks
    ) {
        // 포트의 문서 값을 application result 문서 값으로 변환합니다.
        private static Document from(VitamateAnalysisReaderPort.JobDocument document) {
            return new Document(
                    document.fileVersionId(),
                    document.fileName(),
                    document.documentRole(),
                    document.chunks().stream()
                            .map(Chunk::from)
                            .toList()
            );
        }
    }

    public record Chunk(
            Long documentChunkId,
            String chromaId,
            Integer pageNumber,
            String excerpt
    ) {
        // 포트의 청크 값을 application result 청크 값으로 변환합니다.
        private static Chunk from(VitamateAnalysisReaderPort.JobChunk chunk) {
            return new Chunk(
                    chunk.documentChunkId(),
                    chunk.chromaId(),
                    chunk.pageNumber(),
                    chunk.excerpt()
            );
        }
    }
}
