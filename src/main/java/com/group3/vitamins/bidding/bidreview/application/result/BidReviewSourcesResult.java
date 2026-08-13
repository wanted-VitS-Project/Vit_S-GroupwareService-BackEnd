package com.group3.vitamins.bidding.bidreview.application.result;

import java.util.List;

public record BidReviewSourcesResult(
        Long noticeId,
        List<AttachmentSourceResult> attachments
) {

    public record AttachmentSourceResult(
            Long attachmentId,
            String fileName,
            String sourceType,
            boolean supported
    ) {
    }
}