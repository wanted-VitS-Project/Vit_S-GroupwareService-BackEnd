package com.group3.vitamins.project.stage.application.result;

/**
 * 하위 스텝 권한 일괄 적용 결과.
 * appliedStepCount 는 <b>기존</b> 스텝에 적용된 수다 — 기본값은 항상 저장되므로 0 이어도 실패가 아니다.
 */
public record StageStepPermissionResult(
        Long stageId,
        String userId,
        String permission,
        int appliedStepCount
) {
}
