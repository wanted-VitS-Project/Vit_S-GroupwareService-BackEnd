package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

// bid_notice_raw의 UNIQUE(bid_notice_id, raw_payload_hash)와 대응하는 조회·삽입용 키입니다.
public record RawRecordKey(
        Long bidNoticeId,
        String rawPayload,
        String rawPayloadHash
) {
}
