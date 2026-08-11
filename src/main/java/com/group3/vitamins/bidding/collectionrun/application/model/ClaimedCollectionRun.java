package com.group3.vitamins.bidding.collectionrun.application.model;

import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;

import java.util.Objects;

public record ClaimedCollectionRun(
        Long runId,
        CollectionRunConditionSnapshot conditionSnapshot
) {

    public ClaimedCollectionRun {
        Objects.requireNonNull(runId, "수집 실행 ID는 필수입니다.");
        Objects.requireNonNull(
                conditionSnapshot,
                "수집 조건 스냅샷은 필수입니다."
        );
    }
}