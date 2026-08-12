package com.group3.vitamins.bidding.bidsummary.application.port;

import com.group3.vitamins.bidding.bidsummary.application.model.ClaimedBidNoticeSummaryOutbox;

public interface BidNoticeSummaryJobPublisherPort {

    // 점유된 입찰 AI 요약 작업을 메시지 브로커에 발행합니다.
    void publish(ClaimedBidNoticeSummaryOutbox outbox);
}