package com.group3.vitamins.project.stage.application.usecase;

import java.util.Map;

/**
 * 프로젝트 복제가 하위 스테이지를 복사하기 위해 쓰는 인바운드 유스케이스 (PRJ-018).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(프로젝트 복제)가 원본 참여자 자격을 이미 확인한 뒤 부른다.
 * {@code StageCascadeUseCase} 와 같은 계열이다.
 */
public interface StageCloneUseCase {

    /**
     * 원본 프로젝트의 스테이지를 새 프로젝트로 복사하고 <b>원본 stageId → 새 stageId</b> 매핑을 돌려준다.
     * 스텝 복제가 이 매핑으로 소속을 찾는다.
     *
     * <p>이름과 정렬 순서만 옮긴다. {@code stage_permission_default}(새 스텝 권한 기본값)는
     * 복사하지 않는다 — 참여자를 복제하지 않으므로 그 기본값은 복제본에서 죽은 행이 된다.
     */
    Map<Long, Long> cloneToProject(Long sourceProjectId, Long targetProjectId);
}
