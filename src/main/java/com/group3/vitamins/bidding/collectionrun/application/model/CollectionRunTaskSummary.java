package com.group3.vitamins.bidding.collectionrun.application.model;

public record CollectionRunTaskSummary(
        int totalCount,
        int pendingCount,
        int processingCount,
        int completedCount,
        int failedCount,
        int collectedCount,
        int insertedCount,
        int updatedCount,
        int skippedCount
) {

    // 모든 요청 조합의 처리가 최종 상태에 도달했는지 확인합니다.
    public boolean isFinished() {
        return totalCount > 0 && pendingCount == 0 && processingCount == 0;
    }
}
