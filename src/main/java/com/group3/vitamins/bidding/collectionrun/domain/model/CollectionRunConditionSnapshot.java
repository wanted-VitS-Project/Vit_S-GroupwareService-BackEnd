package com.group3.vitamins.bidding.collectionrun.domain.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;

import java.util.List;
import java.util.Objects;

public record CollectionRunConditionSnapshot(
        String sourceCode,
        String conditionName,
        List<BidNoticeType> noticeTypes,
        CollectionConditionFilter filters
) {

    // 저장 이후 목록이 외부에서 변경되지 않도록 복사합니다.
    public CollectionRunConditionSnapshot {
        Objects.requireNonNull(sourceCode, "수집처 코드는 필수입니다.");
        Objects.requireNonNull(conditionName, "수집 조건명은 필수입니다.");
        Objects.requireNonNull(filters, "수집 필터는 필수입니다.");
        noticeTypes = List.copyOf(noticeTypes);
    }
}
