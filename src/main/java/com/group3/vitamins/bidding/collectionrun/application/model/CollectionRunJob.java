package com.group3.vitamins.bidding.collectionrun.application.model;

import java.util.Objects;

public record CollectionRunJob(
        Long runId,
        Long conditionId,
        Long companyId,
        String attemptId,
        int retryCount
) {
    public CollectionRunJob {
        Objects.requireNonNull(runId, "수집 실행 ID는 필수입니다.");
        Objects.requireNonNull(conditionId, "수집 조건 ID는 필수입니다.");
        Objects.requireNonNull(companyId, "회사 ID는 필수입니다.");
        Objects.requireNonNull(attemptId, "처리 시도 ID는 필수입니다.");
        if (retryCount < 0) {
            throw new IllegalArgumentException("재시도 횟수는 0 이상이어야 합니다.");
        }
    }
}
