package com.group3.vitamins.bidding.bidreview.application.port;

import com.group3.vitamins.bidding.bidreview.application.model.ClaimedBidReviewOutbox;

public interface BidReviewJobPublisherPort {

    // 점유된 입찰 문서 검토 작업(또는 정리 요청)을 메시지 브로커에 발행합니다.
    void publish(ClaimedBidReviewOutbox outbox);
}
