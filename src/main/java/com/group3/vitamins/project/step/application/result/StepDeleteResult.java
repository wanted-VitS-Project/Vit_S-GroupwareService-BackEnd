package com.group3.vitamins.project.step.application.result;

/**
 * 스텝 삭제 결과.
 *
 * <p>재무 연결 해제(BLK-013)는 아직 구현되지 않아 {@code detachedPaymentCount} 계열 필드가 없다 —
 * 재무 접근 경로가 붙을 때 추가한다.
 */
public record StepDeleteResult(
        Long deletedStepId,
        int movedBlockCount,
        int deletedBlockCount,
        int deletedIssueCount
) {
}
