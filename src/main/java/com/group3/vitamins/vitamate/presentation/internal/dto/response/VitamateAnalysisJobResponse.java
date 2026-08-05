package com.group3.vitamins.vitamate.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.application.result.VitamateAnalysisJobDetailResult;

import java.util.List;

// Python worker가 분석을 실행할 때 사용하는 내부 작업 조회 응답
public record VitamateAnalysisJobResponse(
        Long analysisId,
        String attemptId,
        String prompt,
        SearchScope searchScope,
        List<Document> documents
) {

    // application result를 내부 API 응답 DTO로 변환한다.
    public static VitamateAnalysisJobResponse from(VitamateAnalysisJobDetailResult result) {
        return new VitamateAnalysisJobResponse(
                result.analysisId(),
                result.attemptId(),
                result.prompt(),
                SearchScope.from(result.searchScope()),
                result.documents().stream()
                        .map(Document::from)
                        .toList()
        );
    }

    public record SearchScope(
            Long projectId,
            Long blockId,
            List<Long> fileVersionIds
    ) {
        // application 검색 범위 결과를 내부 API 검색 범위 응답으로 변환한다.
        private static SearchScope from(VitamateAnalysisJobDetailResult.SearchScope searchScope) {
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
            List<Chunk> chunks
    ) {
        // application 문서 결과를 내부 API 문서 응답으로 변환한다.
        private static Document from(VitamateAnalysisJobDetailResult.Document document) {
            return new Document(
                    document.fileVersionId(),
                    document.fileName(),
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
        // application 청크 결과를 내부 API 청크 응답으로 변환한다.
        private static Chunk from(VitamateAnalysisJobDetailResult.Chunk chunk) {
            return new Chunk(
                    chunk.documentChunkId(),
                    chunk.chromaId(),
                    chunk.pageNumber(),
                    chunk.excerpt()
            );
        }
    }
}
