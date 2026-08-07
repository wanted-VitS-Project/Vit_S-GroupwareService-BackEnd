package com.group3.vitamins.vitamate.analysis.application.result;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisReaderPort;

import java.time.LocalDateTime;
import java.util.List;

// 비타메이트 분석 상세 조회 결과입니다.
public record VitamateAnalysisDetailResult(
        Long analysisId,
        Long blockId,
        String reviewType,
        List<String> reviewCategoryCodes,
        String additionalInstruction,
        List<TemplateVersion> templateVersions,
        String analysisStatus,
        String result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<Document> documents,
        List<Citation> citations
) {

    // 읽기 포트의 조회 결과를 컨트롤러 응답에 가까운 application result로 변환합니다.
    public static VitamateAnalysisDetailResult from(VitamateAnalysisReaderPort.VitamateAnalysisDetail detail) {
        return new VitamateAnalysisDetailResult(
                detail.analysisId(),
                detail.blockId(),
                detail.reviewType(),
                detail.reviewCategoryCodes(),
                detail.additionalInstruction(),
                detail.templateVersions().stream()
                        .map(TemplateVersion::from)
                        .toList(),
                detail.analysisStatus(),
                detail.result(),
                detail.errorMessage(),
                detail.createdAt(),
                detail.completedAt(),
                detail.documents().stream()
                        .map(Document::from)
                        .toList(),
                detail.citations().stream()
                        .map(Citation::from)
                        .toList()
        );
    }

    public record Document(
            Long fileVersionId,
            String fileName
    ) {
        // 포트의 문서 값을 application result 문서 값으로 변환합니다.
        private static Document from(VitamateAnalysisReaderPort.Document document) {
            return new Document(
                    document.fileVersionId(),
                    document.fileName()
            );
        }
    }

    public record TemplateVersion(
            String categoryCode,
            String templateVersion
    ) {
        // 요청 당시 카테고리별 템플릿 버전을 조회 결과로 변환합니다.
        private static TemplateVersion from(VitamateAnalysisReaderPort.TemplateVersion templateVersion) {
            return new TemplateVersion(
                    templateVersion.categoryCode(),
                    templateVersion.templateVersion()
            );
        }
    }

    public record Citation(
            Integer rankOrder,
            Long fileVersionId,
            Long documentChunkId,
            Integer pageNumber,
            String excerpt
    ) {
        // 포트의 citation 값을 application result citation 값으로 변환합니다.
        private static Citation from(VitamateAnalysisReaderPort.Citation citation) {
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
