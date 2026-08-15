package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

// (crawl_source_id, external_id, notice_ord) 조합의 실제 bid_notice_id를 담는 벌크 조회 결과 행입니다.
public record NoticeIdRow(
        String externalId,
        String noticeOrder,
        Long bidNoticeId
) {
}
