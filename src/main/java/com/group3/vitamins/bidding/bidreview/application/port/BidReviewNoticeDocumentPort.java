package com.group3.vitamins.bidding.bidreview.application.port;

import java.time.LocalDateTime;
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
            String noticeName,
            LocalDateTime bidDeadlineAt
    ) {
    }

    record AttachmentSnapshot(
            Long attachmentId,
            Long noticeId,
            String fileName,
            String sourceUrl,
            String storageKey
    ) {
    }
}