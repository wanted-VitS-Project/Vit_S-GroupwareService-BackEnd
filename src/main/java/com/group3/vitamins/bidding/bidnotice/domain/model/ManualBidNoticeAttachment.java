package com.group3.vitamins.bidding.bidnotice.domain.model;

// 직접 등록 공고에 연결할 공개 원문 첨부 링크입니다.
public record ManualBidNoticeAttachment(
        int attachmentOrder,
        String fileName,
        String sourceUrl
) {
}
