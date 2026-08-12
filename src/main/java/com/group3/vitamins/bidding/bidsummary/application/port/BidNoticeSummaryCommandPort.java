package com.group3.vitamins.bidding.bidsummary.application.port;

import com.group3.vitamins.bidding.bidsummary.application.port
        .BidNoticeSummaryNoticePort.BidNoticeSnapshot;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;

import java.util.Optional;

public interface BidNoticeSummaryCommandPort {

    // 같은 회사·공고·요청자의 진행 중 요약 존재 여부를 확인합니다.
    boolean existsInProgress(
            Long companyId,
            Long noticeId,
            String requestedBy
    );

    // 개선 기준 요약을 잠금 조회하여 생성 중 확정 상태가 바뀌지 않도록 합니다.
    Optional<ImprovementBase> findImprovementBaseForUpdate(
            Long companyId,
            Long noticeId,
            String requestedBy,
            Long summaryId
    );

    // 요약 요청과 Outbox 이벤트를 원자적으로 저장합니다.
    BidNoticeSummary savePendingWithOutbox(
            BidNoticeSummary summary,
            BidNoticeSnapshot noticeSnapshot
    );

    record ImprovementBase(
            Long summaryId,
            int revisionNo,
            BidNoticeSummaryStatus summaryStatus,
            boolean confirmed
    ) {
    }
}
