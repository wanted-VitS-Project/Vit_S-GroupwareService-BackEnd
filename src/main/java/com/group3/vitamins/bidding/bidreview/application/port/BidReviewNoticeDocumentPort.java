package com.group3.vitamins.bidding.bidreview.application.port;

import java.util.List;
import java.util.Optional;

public interface BidReviewNoticeDocumentPort {

    Optional<NoticeSnapshot> findAccessibleNotice(
            Long companyId,
            Long noticeId
    );

    List<AttachmentSnapshot> findAttachments(
            Long companyId,
            Long noticeId,
            List<Long> attachmentIds
    );

    record NoticeSnapshot(
            Long noticeId,
            String noticeName
    ) {
    }

    record AttachmentSnapshot(
            Long attachmentId,
            Long noticeId,
            String fileName,
            String sourceUrl
    ) {
    }
}