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
        List<ReferenceFileJob> referenceFiles
) {

    public record AttachmentJob(
            Long attachmentId,
            String fileName,
            String sourceUrl
    ) {
    }

    public record ReferenceFileJob(
            Long referenceFileId,
            String fileName,
            String downloadUrl
    ) {
    }
}
