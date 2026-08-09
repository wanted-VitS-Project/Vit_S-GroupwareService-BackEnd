package com.group3.vitamins.project.step.application.result;

/** 권한 부여·회수 결과. 회수 응답의 permission 은 상속으로 되돌아간 등급이다. */
public record StepPermissionResult(
        Long stepId,
        String userId,
        String permission,
        boolean overridden
) {
}
