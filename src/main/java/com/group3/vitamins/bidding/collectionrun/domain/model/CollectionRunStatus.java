package com.group3.vitamins.bidding.collectionrun.domain.model;

public enum CollectionRunStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    PARTIAL_SUCCESS,
    FAILED;

    // 아직 종료되지 않아 같은 조건의 중복 실행을 막아야 하는 상태인지 반환합니다.
    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }
}
