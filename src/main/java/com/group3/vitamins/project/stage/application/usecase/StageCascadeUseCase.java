package com.group3.vitamins.project.stage.application.usecase;

/**
 * 프로젝트 삭제가 하위 스테이지를 정리하기 위해 쓰는 인바운드 유스케이스 (PRJ-014).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(프로젝트 삭제)가 이미 프로젝트 EDITOR 를 확인한 뒤 부른다.
 * {@link StageCommandUseCase#deleteStage} 를 그대로 쓰면 스테이지마다 권한을 다시 묻고,
 * 게다가 하위 스텝 이전 대상({@code moveToStageId})을 요구해서 프로젝트 삭제에는 쓸 수 없다.
 * {@code BlockCascadeUseCase}·{@code IssueCascadeUseCase} 와 같은 계열이다.
 */
public interface StageCascadeUseCase {

    /**
     * 프로젝트의 스테이지를 권한 기본값과 함께 정리한다. 논리 삭제한 스테이지 수를 돌려준다.
     *
     * <p>하위 스텝은 건드리지 않는다 — 프로젝트 삭제가 {@code StepCascadeUseCase} 로 <b>먼저</b>
     * 정리하고 부른다. 스테이지 단건 삭제(STG-003)도 스텝을 지우지 않는 것과 같은 규칙이다.
     */
    int deleteByProjectId(Long projectId);
}
