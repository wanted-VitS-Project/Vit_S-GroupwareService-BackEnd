package com.group3.vitamins.vitamate.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.application.command.HandleVitamateAnalysisCallbackCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

// Python worker가 분석 처리 후 전달하는 callback 요청 DTO
@Schema(description = "Python 분석 결과 callback 요청")
public record VitamateAnalysisCallbackRequest(
        @Schema(description = "현재 워커 실행 토큰", example = "9f6c3e6b-8974-4f8d-8c88-2e1d3e0d3138")
        @NotBlank
        String attemptId,

        @Schema(description = "분석 처리 결과 상태", allowableValues = {"COMPLETED", "FAILED"}, example = "COMPLETED")
        @NotBlank
        String analysisStatus,

        @Schema(description = "분석 결과. COMPLETED면 필수, FAILED면 null", example = "핵심 기술 요구사항은 통합 관제와 실시간 데이터 분석입니다.")
        String result,

        @Schema(description = "검색 근거 청크 목록. COMPLETED면 빈 배열 가능, FAILED면 빈 배열")
        List<@NotNull @Valid Citation> citations,

        @Schema(description = "오류 메시지. FAILED면 필수, COMPLETED면 null", example = "문서 청크를 찾을 수 없습니다.")
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
    @Schema(description = "Python 분석 근거 청크")
    public record Citation(
            @Schema(description = "근거 청크 ID", example = "9001")
            @NotNull
            @Positive
            Long documentChunkId,

            @Schema(description = "근거 청크가 속한 파일 버전 ID", example = "101")
            @NotNull
            @Positive
            Long fileVersionId,

            @Schema(description = "근거 순서", example = "1")
            @NotNull
            @Positive
            Integer rankOrder,

            @Schema(description = "검색 거리 점수", example = "0.14321")
            @DecimalMin("0.0")
            BigDecimal distanceScore,

            @Schema(description = "근거 발췌문", example = "통합 관제 플랫폼 구축...")
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
