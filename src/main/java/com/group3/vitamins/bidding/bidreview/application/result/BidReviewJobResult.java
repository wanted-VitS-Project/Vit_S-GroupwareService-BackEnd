package com.group3.vitamins.bidding.bidreview.application.result;

import java.util.List;

public record BidReviewJobResult(
        Long reviewId,
        Long companyId,
        String attemptId,
        String prompt,
        Long noticeId,
        String noticeName,
        List<AttachmentJob> attachments,
        List<ReferenceFileJob> referenceFiles,
        List<CompanyDocumentJob> companyDocuments,
        String qualificationSummary
) {

    public record AttachmentJob(
            Long attachmentId,
            String fileName,
            String sourceUrl,
            String uploadUrl,
            String temporaryStorageKey
    ) {
    }

    public record ReferenceFileJob(
            Long referenceFileId,
            String fileName,
            String downloadUrl
    ) {
    }

    public record CompanyDocumentJob(
            Long companyDocumentVersionId,
            String fileName,
            String downloadUrl
    ) {
    }
}
