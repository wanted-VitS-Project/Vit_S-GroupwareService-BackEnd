package com.group3.vitamins.bidding.bidreview.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BidReviewDetailQueryPort {

    Optional<ReviewRow> findReview(Long reviewId);

    List<DocumentRow> findDocuments(Long reviewId);

    List<CitationRow> findCitations(Long reviewId);

    record ReviewRow(
            Long reviewId,
            Long companyId,
            Long noticeId,
            String requestedBy,
            String prompt,
            String reviewStatus,
            String result,
            String errorMessage,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            LocalDateTime expiresAt,
            Long projectId
    ) {
    }

    record DocumentRow(
            String documentRole,
            Long bidAttachmentId,
            Long referenceFileId,
            Long companyDocumentVersionId,
            String fileName,
            String processingStatus
    ) {
    }

    record CitationRow(
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