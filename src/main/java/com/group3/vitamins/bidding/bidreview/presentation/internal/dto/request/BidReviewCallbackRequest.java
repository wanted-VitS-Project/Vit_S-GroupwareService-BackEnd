package com.group3.vitamins.bidding.bidreview.presentation.internal.dto.request;

import com.group3.vitamins.bidding.bidreview.application.command.HandleBidReviewCallbackCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BidReviewCallbackRequest(

        @NotBlank
        @Schema(description = "현재 처리 시도 ID")
        String attemptId,

        @NotBlank
        @Schema(
                description = "처리 결과 상태",
                allowableValues = {"PROCESSING", "COMPLETED", "FAILED"}
        )
        String reviewStatus,

        @Schema(description = "근거 번호를 포함한 검토 결과. COMPLETED이면 필수")
        String result,

        @Schema(description = "오류 분류 코드. FAILED이면 사용")
        String errorCode,

        @Schema(description = "실패 메시지. FAILED이면 필수")
        String errorMessage,

        @Schema(
                description = "일시 장애로 재시도할 수 있는지 여부",
                example = "false",
                defaultValue = "false"
        )
        Boolean retryable,

        @Schema(description = "공고 첨부별 임시 저장·처리 결과")
        List<DocumentOutcomeRequest> documents,

        @Schema(description = "COMPLETED 검토 근거 목록")
        List<CitationRequest> citations
) {

    // HTTP 요청을 callback 처리 Command로 변환합니다.
    public HandleBidReviewCallbackCommand toCommand(Long reviewId) {
        return new HandleBidReviewCallbackCommand(
                reviewId,
                attemptId,
                reviewStatus,
                result,
                errorCode,
                errorMessage,
                Boolean.TRUE.equals(retryable),
                toDocuments(),
                toCitations()
        );
    }

    private List<HandleBidReviewCallbackCommand.DocumentOutcomeInput> toDocuments() {
        if (documents == null) {
            return null;
        }

        return documents.stream()
                .map(DocumentOutcomeRequest::toCommand)
                .toList();
    }

    private List<HandleBidReviewCallbackCommand.CitationInputCommand> toCitations() {
        if (citations == null) {
            return null;
        }

        return citations.stream()
                .map(CitationRequest::toCommand)
                .toList();
    }

    public record DocumentOutcomeRequest(
            @Schema(description = "공고 첨부 ID")
            Long bidAttachmentId,

            @Schema(description = "문서 처리 상태")
            String processingStatus,

            @Schema(description = "임시 저장소 키(Spring DB 내부용)")
            String temporaryStorageKey,

            @Schema(description = "다운로드된 파일 크기")
            Long fileSize,

            @Schema(description = "MIME 타입")
            String mimeType
    ) {

        HandleBidReviewCallbackCommand.DocumentOutcomeInput toCommand() {
            return new HandleBidReviewCallbackCommand.DocumentOutcomeInput(
                    bidAttachmentId,
                    processingStatus,
                    temporaryStorageKey,
                    fileSize,
                    mimeType
            );
        }
    }

    public record CitationRequest(
            @Schema(description = "근거 순위")
            int rankOrder,

            @Schema(description = "문서 역할", allowableValues = {"BID_ATTACHMENT", "INTERNAL_REFERENCE"})
            String documentRole,

            @Schema(description = "공고 첨부 ID")
            Long bidAttachmentId,

            @Schema(description = "입찰 기준자료 ID")
            Long referenceFileId,

            @Schema(description = "파일명")
            String fileName,

            @Schema(description = "페이지 번호")
            Integer pageNumber,

            @Schema(description = "시트명")
            String sheetName,

            @Schema(description = "발췌문")
            String excerpt
    ) {

        HandleBidReviewCallbackCommand.CitationInputCommand toCommand() {
            return new HandleBidReviewCallbackCommand.CitationInputCommand(
                    rankOrder,
                    documentRole,
                    bidAttachmentId,
                    referenceFileId,
                    fileName,
                    pageNumber,
                    sheetName,
                    excerpt
            );
        }
    }
}
