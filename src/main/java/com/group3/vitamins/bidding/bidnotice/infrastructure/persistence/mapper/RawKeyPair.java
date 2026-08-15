package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

// bid_notice_raw의 UNIQUE(bid_notice_id, raw_payload_hash) 존재 여부 확인에만 쓰는 경량 키입니다.
public record RawKeyPair(
        Long bidNoticeId,
        String rawPayloadHash
) {
}
