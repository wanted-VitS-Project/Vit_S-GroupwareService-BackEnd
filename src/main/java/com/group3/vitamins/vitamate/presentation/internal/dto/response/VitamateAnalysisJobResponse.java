package com.group3.vitamins.vitamate.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.application.result.VitamateAnalysisJobDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Python worker가 분석을 실행할 때 사용하는 내부 작업 조회 응답
@Schema(description = "Python worker 분석 작업 조회 응답")
public record VitamateAnalysisJobResponse(
        @Schema(description = "Spring Boot에서 생성한 분석 ID", example = "501")
        Long analysisId,

        @Schema(description = "현재 워커 실행 토큰", example = "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138")
        String attemptId,

        @Schema(description = "분석 프롬프트", example = "핵심 기술 요구사항과 위험 요소를 정리해줘.")
        String prompt,

        @Schema(description = "검색 범위")
        SearchScope searchScope,

        @Schema(description = "선택 문서와 청크 후보")
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
            @Schema(description = "검색 범위 프로젝트 ID", example = "10")
            Long projectId,

            @Schema(description = "요청이 발생한 비타메이트 블록 ID", example = "30")
            Long blockId,

            @Schema(description = "선택된 파일 버전 ID 목록", example = "[101, 102]")
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
            @Schema(description = "파일 버전 ID", example = "101")
            Long fileVersionId,

            @Schema(description = "파일명", example = "제안요청서.pdf")
            String fileName,

            @Schema(description = "검색 후보 청크 목록")
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
            @Schema(description = "문서 청크 ID", example = "9001")
            Long documentChunkId,

            @Schema(description = "ChromaDB 식별자", example = "fv101-chunk-1")
            String chromaId,

            @Schema(description = "페이지 번호", example = "3")
            Integer pageNumber,

            @Schema(description = "청크 미리보기", example = "사업 범위는...")
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
