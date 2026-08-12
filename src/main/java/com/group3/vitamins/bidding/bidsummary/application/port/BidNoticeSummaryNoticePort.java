package com.group3.vitamins.bidding.bidsummary.application.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BidNoticeSummaryNoticePort {

    // 회사가 접근할 수 있는 공고의 AI 요약용 스냅샷을 조회합니다.
    Optional<BidNoticeSnapshot> findAccessibleNotice(
            Long companyId,
            Long noticeId
    );

    record BidNoticeSnapshot(
            Long noticeId,
            String noticeName,
            String noticeType,
            String noticeAgency,
            String demandAgency,
            BigDecimal baseAmount,
            BigDecimal estimatedAmount,
            LocalDateTime announcedAt,
            LocalDateTime bidStartAt,
            LocalDateTime bidDeadlineAt,
            LocalDateTime openingAt,
            String participationQualificationText,
            String regionLimitText,
            String businessLimitText,
            String contractMethod,
            String evaluationMethod,
            String sourceUrl,
            List<AttachmentSnapshot> attachments
    ) {
        public BidNoticeSnapshot {
            attachments = attachments == null
                    ? List.of()
                    : List.copyOf(attachments);
        }
    }

    record AttachmentSnapshot(
            Short attachmentOrder,
            String fileName,
            String sourceUrl
    ) {
    }
}