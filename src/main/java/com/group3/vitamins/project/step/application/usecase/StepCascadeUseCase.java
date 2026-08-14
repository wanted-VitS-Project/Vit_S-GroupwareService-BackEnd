package com.group3.vitamins.project.step.application.usecase;

/**
 * 프로젝트 삭제가 하위 스텝을 정리하기 위해 쓰는 인바운드 유스케이스 (PRJ-014).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(프로젝트 삭제)가 이미 프로젝트 EDITOR 를 확인한 뒤 부른다.
 * {@link StepCommandUseCase#deleteStep} 을 그대로 쓰면 스텝마다 권한을 다시 묻고, 게다가 블록 이전
 * 대상({@code moveToStepId})을 요구해서 프로젝트 삭제에는 쓸 수 없다.
 * {@code StageCascadeUseCase}·{@code BlockCascadeUseCase} 와 같은 계열이다.
 */
public interface StepCascadeUseCase {

    /**
     * 프로젝트의 스텝을 하위 블록·이슈·권한 오버라이드와 함께 정리한다. 논리 삭제한 스텝 수를 돌려준다.
     *
     * <p>블록을 옮기는 선택지는 없다 — 프로젝트째 사라지므로 옮길 곳이 없다.
     */
    int deleteByProjectId(Long projectId, String requesterUserId);
}
