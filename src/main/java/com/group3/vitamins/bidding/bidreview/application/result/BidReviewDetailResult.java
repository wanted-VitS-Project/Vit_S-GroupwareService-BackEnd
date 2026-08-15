package com.group3.vitamins.bidding.bidreview.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record BidReviewDetailResult(
        Long reviewId,
        Long noticeId,
        String prompt,
        String reviewStatus,
        String result,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt,
        Long projectId,
        int retryCount,
        List<DocumentResult> documents,
        List<CitationResult> citations
) {

    public record DocumentResult(
            String documentRole,
            Long bidAttachmentId,
            Long referenceFileId,
            Long companyDocumentVersionId,
            String fileName,
            String processingStatus
    ) {
    }

    public record CitationResult(
            Integer rankOrder,
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