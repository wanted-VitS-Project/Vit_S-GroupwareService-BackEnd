package com.group3.vitamins.bidding.bidreview.application.command;

import java.util.List;

public record HandleBidReviewCallbackCommand(
        Long reviewId,
        String attemptId,
        String reviewStatus,
        String result,
        String errorCode,
        String errorMessage,
        boolean retryable,
        List<DocumentOutcomeInput> documents,
        List<CitationInputCommand> citations
) {

    public record DocumentOutcomeInput(
            Long bidAttachmentId,
            String processingStatus,
            String temporaryStorageKey,
            Long fileSize,
            String mimeType
    ) {
    }

    public record CitationInputCommand(
            int rankOrder,
            String documentRole,
            Long bidAttachmentId,
            Long referenceFileId,
            Long companyDocumentVersionId,
            String fileName,
            Integer pageNumber,
            String sheetName,
            String excerpt
    ) {
    }
}
