package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;

// hasAttachments()는 record 구성요소가 아니라 파생 메서드라 MyBatis OGNL로 안전하게
// 접근할 수 없어, 어댑터가 미리 계산해 평범한 필드로 넘긴다.
public record NoticeUpsertRow(
        CollectedBidNotice notice,
        boolean hasAttachments
) {
}
