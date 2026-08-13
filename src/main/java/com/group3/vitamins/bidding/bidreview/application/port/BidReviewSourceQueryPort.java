package com.group3.vitamins.bidding.bidreview.application.port;

import java.util.List;

public interface BidReviewSourceQueryPort {

    List<AttachmentSource> findAttachmentSources(Long companyId, Long noticeId);

    record AttachmentSource(
            Long attachmentId,
            String fileName,
            String sourceType
    ) {
    }
}