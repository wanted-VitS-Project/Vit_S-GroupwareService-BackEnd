package com.group3.vitamins.project.step.application.result;

/**
 * 참여자 한 명의 스텝 권한 판정 결과.
 * {@code overridden} 이 false 면 step_permission 행이 없다는 뜻이고, 값은 프로젝트 권한 상속이다 (STP-011).
 */
public record StepPermissionSummary(
        String userId,
        String name,
        String permission,
        boolean overridden
) {
}
