package com.group3.vitamins.project.step.application.usecase;

/**
 * 스텝 권한 오버라이드 정리를 상위 애그리게이트(project)에 제공하는 인바운드 유스케이스.
 * 소비자가 {@code step_permission} 을 직접 쓰지 않게 한다 — 쓰기 경로는 스텝 애그리게이트가 소유한다.
 */
public interface StepPermissionCleanupUseCase {

    /** 프로젝트에 속한 스텝들에서 그 사용자의 오버라이드를 전부 지운다. 없으면 아무 일도 하지 않는다. */
    void deleteByProjectIdAndUserId(Long projectId, String userId);
}
