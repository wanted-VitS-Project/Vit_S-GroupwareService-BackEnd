package com.group3.vitamins.vitamate.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.application.command.HandleVitamateAnalysisCallbackCommand;

import java.math.BigDecimal;
import java.util.List;

// Python worker가 분석 처리 후 전달하는 callback 요청 DTO
public record VitamateAnalysisCallbackRequest(
        String attemptId,
        String analysisStatus,
        String result,
        List<Citation> citations,
        String errorMessage
) {

    // HTTP 요청 DTO를 application command로 변환한다.
    public HandleVitamateAnalysisCallbackCommand toCommand(Long analysisId) {
        List<HandleVitamateAnalysisCallbackCommand.Citation> commandCitations =
                citations == null
                        ? List.of()
                        : citations.stream()
                        .map(Citation::toCommand)
                        .toList();

        return new HandleVitamateAnalysisCallbackCommand(
                analysisId,
                attemptId,
                analysisStatus,
                result,
                commandCitations,
                errorMessage
        );
    }

    // Python worker가 반환한 분석 근거 요청 DTO
    public record Citation(
            Long documentChunkId,
            Long fileVersionId,
            Integer rankOrder,
            BigDecimal distanceScore,
            String excerpt
    ) {

        // callback citation 요청 값을 application command 값으로 변환한다.
        private HandleVitamateAnalysisCallbackCommand.Citation toCommand() {
            return new HandleVitamateAnalysisCallbackCommand.Citation(
                    documentChunkId,
                    fileVersionId,
                    rankOrder,
                    distanceScore,
                    excerpt
            );
        }
    }
}
