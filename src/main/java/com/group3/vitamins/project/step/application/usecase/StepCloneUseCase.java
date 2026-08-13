package com.group3.vitamins.project.step.application.usecase;

import java.util.Map;

/**
 * 프로젝트 복제가 하위 스텝을 복사하기 위해 쓰는 인바운드 유스케이스 (PRJ-018).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(프로젝트 복제)가 원본 참여자 자격을 이미 확인한 뒤 부른다.
 * {@code StepCascadeUseCase} 와 같은 계열이다.
 */
public interface StepCloneUseCase {

    /**
     * 원본 프로젝트의 스텝을 새 프로젝트로 복사하고 <b>원본 stepId → 새 stepId</b> 매핑을 돌려준다.
     * 블록 복제가 이 매핑으로 소속을 찾는다.
     *
     * <p>이름과 정렬 순서만 옮긴다 — 기간·책임자·상태·완료정보는 복사하지 않고 {@code NOT_STARTED} 로 시작한다.
     * {@code step_permission} 오버라이드도 복사하지 않는다(사람을 옮기지 않으므로).
     *
     * <p>미소속 스텝({@code stage_id IS NULL})은 <b>미소속 그대로</b> 복사된다.
     *
     * @param stageIdMap 원본 stageId → 새 stageId
     */
    Map<Long, Long> cloneToProject(Long sourceProjectId, Long targetProjectId,
                                   Map<Long, Long> stageIdMap);
}
