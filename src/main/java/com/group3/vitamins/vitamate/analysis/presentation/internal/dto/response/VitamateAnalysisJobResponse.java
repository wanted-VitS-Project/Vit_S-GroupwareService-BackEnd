package com.group3.vitamins.vitamate.analysis.presentation.internal.dto.response;

import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisJobDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Python worker가 분석을 실행할 때 사용하는 내부 작업 조회 응답입니다.
@Schema(description = "Python worker 분석 작업 조회 응답")
public record VitamateAnalysisJobResponse(
        @Schema(description = "Spring Boot에서 생성한 분석 ID", example = "501")
        Long analysisId,

        @Schema(description = "현재 워커 실행 토큰", example = "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138")
        String attemptId,

        @Schema(description = "검토 유형", example = "COST_REPORT")
        String reviewType,

        @Schema(description = "선택한 검토 카테고리 코드 목록", example = "[\"COMMON\", \"COST_RESULT\"]")
        List<String> reviewCategoryCodes,

        @Schema(description = "사용자가 기본 템플릿을 확인·보완해 확정한 최종 프롬프트. worker가 그대로 사용한다")
        String prompt,

        @Schema(description = "분석 요청 당시 선택한 검토 템플릿 스냅샷")
        List<ReviewTemplate> reviewTemplates,

        @Schema(description = "검색 범위")
        SearchScope searchScope,

        @Schema(description = "선택 문서와 청크 후보")
        List<Document> documents
) {

    // application result를 내부 API 응답 DTO로 변환합니다.
    public static VitamateAnalysisJobResponse from(VitamateAnalysisJobDetailResult result) {
        return new VitamateAnalysisJobResponse(
                result.analysisId(),
                result.attemptId(),
                result.reviewType(),
                result.reviewCategoryCodes(),
                result.prompt(),
                result.reviewTemplates().stream()
                        .map(ReviewTemplate::from)
                        .toList(),
                SearchScope.from(result.searchScope()),
                result.documents().stream()
                        .map(Document::from)
                        .toList()
        );
    }

    public record ReviewTemplate(
            @Schema(description = "검토 유형 코드", example = "COST_REPORT")
            String reviewType,

            @Schema(description = "검토 카테고리 코드", example = "COST_RESULT")
            String categoryCode,

            @Schema(description = "검토 카테고리 이름", example = "I. 원가계산 결과")
            String categoryName,

            @Schema(description = "Python worker가 사용할 프롬프트 템플릿")
            String promptTemplate,

            @Schema(description = "템플릿 버전", example = "COST_REPORT_V1")
            String templateVersion
    ) {
        // application 템플릿 결과를 내부 API 응답 값으로 변환합니다.
        private static ReviewTemplate from(VitamateAnalysisJobDetailResult.ReviewTemplate template) {
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
            @Schema(description = "검색 범위 프로젝트 ID", example = "10")
            Long projectId,

            @Schema(description = "요청이 발생한 비타메이트 블록 ID", example = "30")
            Long blockId,

            @Schema(description = "선택한 파일 버전 ID 목록", example = "[101, 102]")
            List<Long> fileVersionIds
    ) {
        // application 검색 범위 결과를 내부 API 검색 범위 응답으로 변환합니다.
        private static SearchScope from(VitamateAnalysisJobDetailResult.SearchScope searchScope) {
            return new SearchScope(
                    searchScope.projectId(),
                    searchScope.blockId(),
                    searchScope.fileVersionIds()
            );
        }
    }

    @Schema(name = "VitamateAnalysisJobResponseDocument")
    public record Document(
            @Schema(description = "파일 버전 ID", example = "101")
            Long fileVersionId,

            @Schema(description = "파일명", example = "제안요청서.pdf")
            String fileName,

            @Schema(description = "문서 역할", example = "TARGET")
            String documentRole,

            @Schema(description = "검색 후보 청크 목록")
            List<Chunk> chunks
    ) {
        // application 문서 결과를 내부 API 문서 응답으로 변환합니다.
        private static Document from(VitamateAnalysisJobDetailResult.Document document) {
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
            @Schema(description = "문서 청크 ID", example = "9001")
            Long documentChunkId,

            @Schema(description = "ChromaDB 식별자", example = "fv101-chunk-1")
            String chromaId,

            @Schema(description = "페이지 번호", example = "3")
            Integer pageNumber,

            @Schema(description = "청크 미리보기", example = "사업 범위는...")
            String excerpt
    ) {
        // application 청크 결과를 내부 API 청크 응답으로 변환합니다.
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
