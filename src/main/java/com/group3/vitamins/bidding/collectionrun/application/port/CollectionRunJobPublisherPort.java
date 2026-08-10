package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.ClaimedCollectionRunOutbox;

public interface CollectionRunJobPublisherPort {

    // 점유한 입찰 수집 작업을 외부 메시지 브로커에 발행합니다.
    void publish(ClaimedCollectionRunOutbox outbox);
}
