package com.group3.vitamins.vitamate.analysis.presentation.api.dto.response;

import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

// 비타메이트 분석 상태와 결과 조회 응답입니다.
@Schema(description = "비타메이트 분석 상태 및 결과 조회 응답")
public record VitamateAnalysisResponse(

        @Schema(description = "분석 ID", example = "501")
        Long analysisId,

        @Schema(description = "비타메이트 블록 ID", example = "12")
        Long blockId,

        @Schema(description = "검토 유형", example = "COST_REPORT")
        String reviewType,

        @Schema(description = "선택한 검토 카테고리 코드 목록", example = "[\"COMMON\", \"COST_RESULT\"]")
        List<String> reviewCategoryCodes,

        @Schema(description = "사용자 추가 요청. 없으면 null", example = "금액과 부가세 포함 여부를 특히 확인해줘.")
        String additionalInstruction,

        @Schema(description = "Python worker가 적용한 검토 템플릿 버전", example = "COST_REPORT_V1")
        String promptTemplateVersion,

        @Schema(description = "분석 상태", example = "COMPLETED")
        String analysisStatus,

        @Schema(description = "분석 결과. PENDING, PROCESSING, FAILED 상태에서는 null")
        String result,

        @Schema(description = "실패 메시지. FAILED 상태가 아니면 null")
        String errorMessage,

        @Schema(description = "생성 시각", example = "2026-08-04T14:05:00")
        LocalDateTime createdAt,

        @Schema(description = "처리 완료 시각. 진행 중이면 null")
        LocalDateTime completedAt,

        @Schema(description = "분석 대상 문서 목록. 권한 있는 200 응답에서는 항상 배열")
        List<Document> documents,

        @Schema(description = "분석 근거 목록. 없으면 빈 배열")
        List<Citation> citations
) {

    // application 결과 객체를 HTTP 응답 DTO로 변환합니다.
    public static VitamateAnalysisResponse from(VitamateAnalysisDetailResult result) {
        return new VitamateAnalysisResponse(
                result.analysisId(),
                result.blockId(),
                result.reviewType(),
                result.reviewCategoryCodes(),
                result.additionalInstruction(),
                result.promptTemplateVersion(),
                result.analysisStatus(),
                result.result(),
                result.errorMessage(),
                result.createdAt(),
                result.completedAt(),
                result.documents().stream()
                        .map(Document::from)
                        .toList(),
                result.citations().stream()
                        .map(Citation::from)
                        .toList()
        );
    }

    public record Document(
            @Schema(description = "파일 버전 ID", example = "101")
            Long fileVersionId,

            @Schema(description = "파일명", example = "스마트시티_제안서_v2.pdf")
            String fileName
    ) {
        // application 문서 결과를 HTTP 응답 문서 값으로 변환합니다.
        private static Document from(VitamateAnalysisDetailResult.Document document) {
            return new Document(
                    document.fileVersionId(),
                    document.fileName()
            );
        }
    }

    public record Citation(
            @Schema(description = "근거 순서", example = "1")
            Integer rankOrder,

            @Schema(description = "근거 청크가 속한 파일 버전 ID", example = "101")
            Long fileVersionId,

            @Schema(description = "문서 청크 ID", example = "3001")
            Long documentChunkId,

            @Schema(description = "페이지 번호", example = "4")
            Integer pageNumber,

            @Schema(description = "근거 발췌문", example = "통합 관제 플랫폼 구축은 핵심 요구사항이다.")
            String excerpt
    ) {
        // application citation 결과를 HTTP 응답 citation 값으로 변환합니다.
        private static Citation from(VitamateAnalysisDetailResult.Citation citation) {
            return new Citation(
                    citation.rankOrder(),
                    citation.fileVersionId(),
                    citation.documentChunkId(),
                    citation.pageNumber(),
                    citation.excerpt()
            );
        }
    }
}
