package com.group3.vitamins.bidding.bidnotice.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record CompanyBidNoticeState(
        Long companyId,
        Long noticeId,
        BidNoticeCompanyStatus status,
        String dismissReason,
        boolean isFavorite,
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
                isFavorite,
                changedAt
        );
    }

    public CompanyBidNoticeState restore(LocalDateTime changedAt) {
        return new CompanyBidNoticeState(
                companyId,
                noticeId,
                BidNoticeCompanyStatus.COLLECTED,
                null,
                isFavorite,
                changedAt
        );
    }

    // 회사 공용 관심 등록 - notice_status와 독립적이라 제외(DISMISSED) 상태에서도 걸 수 있다.
    public CompanyBidNoticeState markFavorite(LocalDateTime changedAt) {
        return new CompanyBidNoticeState(
                companyId,
                noticeId,
                status,
                dismissReason,
                true,
                changedAt
        );
    }

    public CompanyBidNoticeState unmarkFavorite(LocalDateTime changedAt) {
        return new CompanyBidNoticeState(
                companyId,
                noticeId,
                status,
                dismissReason,
                false,
                changedAt
        );
    }
}
