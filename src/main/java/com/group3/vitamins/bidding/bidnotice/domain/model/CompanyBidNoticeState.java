package com.group3.vitamins.bidding.bidnotice.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record CompanyBidNoticeState(
        Long companyId,
        Long noticeId,
        BidNoticeCompanyStatus status,
        String dismissReason,
        LocalDateTime updatedAt
) {
    public CompanyBidNoticeState {
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(noticeId, "입찰 공고 ID는 필수입니다.");
        Objects.requireNonNull(status, "입찰 공고 상태는 필수입니다.");
    }

    public CompanyBidNoticeState dismiss(String reason, LocalDateTime changedAt) {
        return new CompanyBidNoticeState(
                companyId,
                noticeId,
                BidNoticeCompanyStatus.DISMISSED,
                reason,
                changedAt
        );
    }

    public CompanyBidNoticeState restore(LocalDateTime changedAt) {
        return new CompanyBidNoticeState(
                companyId,
                noticeId,
                BidNoticeCompanyStatus.COLLECTED,
                null,
                changedAt
        );
    }
}
