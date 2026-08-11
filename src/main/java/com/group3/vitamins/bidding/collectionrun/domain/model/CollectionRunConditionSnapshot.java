package com.group3.vitamins.bidding.collectionrun.domain.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record CollectionRunConditionSnapshot(
        String sourceCode,
        String conditionName,
        List<BidNoticeType> noticeTypes,
        CollectionConditionFilter filters,
        LocalDateTime collectionStartedAt,
        LocalDateTime collectionEndedAt
) {

    // 저장 이후 목록이 외부에서 변경되지 않도록 복사합니다.
    public CollectionRunConditionSnapshot {
        Objects.requireNonNull(sourceCode, "수집처 코드는 필수입니다.");
        Objects.requireNonNull(conditionName, "수집 조건명은 필수입니다.");
        Objects.requireNonNull(noticeTypes, "공고 유형 목록은 필수입니다.");
        Objects.requireNonNull(filters, "수집 필터는 필수입니다.");
        Objects.requireNonNull(collectionStartedAt, "수집 시작 시각은 필수입니다.");
        Objects.requireNonNull(collectionEndedAt, "수집 종료 시각은 필수입니다.");
        if (collectionStartedAt.isAfter(collectionEndedAt)) {
            throw new IllegalArgumentException(
                    "수집 시작 시각은 종료 시각보다 늦을 수 없습니다."
            );
        }
        noticeTypes = List.copyOf(noticeTypes);
    }
}
