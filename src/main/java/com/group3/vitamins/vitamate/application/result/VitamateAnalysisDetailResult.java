package com.group3.vitamins.vitamate.application.result;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisReader;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 비타메이트 분석 상세 조회 결과입니다.
 */
public record VitamateAnalysisDetailResult(
        Long analysisId,
        Long blockId,
        String prompt,
        String analysisStatus,
        String result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<Document> documents,
        List<Citation> citations
) {

    // 읽기 포트의 조회 결과를 컨트롤러 응답에 가까운 application result로 변환한다.
    public static VitamateAnalysisDetailResult from(VitamateAnalysisReader.VitamateAnalysisDetail detail) {
        return new VitamateAnalysisDetailResult(
                detail.analysisId(),
                detail.blockId(),
                detail.prompt(),
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
        // 포트의 문서 값을 application result 문서 값으로 변환한다.
        private static Document from(VitamateAnalysisReader.Document document) {
            return new Document(
                    document.fileVersionId(),
                    document.fileName()
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
        // 포트의 citation 값을 application result citation 값으로 변환한다.
        private static Citation from(VitamateAnalysisReader.Citation citation) {
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
