package com.group3.vitamins.bidding.bidnotice.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record BidNoticeStatusHistory(
        Long companyId,
        Long noticeId,
        BidNoticeCompanyStatus previousStatus,
        BidNoticeCompanyStatus changedStatus,
        String reason,
        String changedBy,
        LocalDateTime createdAt
) {
    public BidNoticeStatusHistory {
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(noticeId, "입찰 공고 ID는 필수입니다.");
        Objects.requireNonNull(changedStatus, "변경 후 상태는 필수입니다.");
        Objects.requireNonNull(changedBy, "작업자 ID는 필수입니다.");
        Objects.requireNonNull(createdAt, "변경 시각은 필수입니다.");
    }
}
