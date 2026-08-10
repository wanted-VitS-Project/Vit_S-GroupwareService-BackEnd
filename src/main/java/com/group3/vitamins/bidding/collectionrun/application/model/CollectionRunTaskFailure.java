package com.group3.vitamins.bidding.collectionrun.application.model;

// 최종 실패한 수집 Task를 DLQ에 기록할 때 필요한 안전한 정보입니다.
public record CollectionRunTaskFailure(
        Long runId,
        Long taskId,
        Long companyId,
        String attemptId,
        int retryCount,
        CollectionRunFailureType failureType,
        CollectionRequestCombination target
) {
}
