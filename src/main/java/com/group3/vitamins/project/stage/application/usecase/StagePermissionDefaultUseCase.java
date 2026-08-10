package com.group3.vitamins.project.stage.application.usecase;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.util.Map;

/**
 * 스테이지 「새 스텝 권한 기본값」을 다른 애그리게이트(step · project)에 노출하는 인바운드 유스케이스.
 *
 * <p>⚠️ 이 값은 <b>권한 판정에 쓰이지 않는다</b> (INV-01). 스텝 생성 시 복사되고, 정리 시 삭제될 뿐이다.
 */
public interface StagePermissionDefaultUseCase {

    /** 스테이지의 기본값 전부. 사번 → 권한. 스텝 생성 시 이걸 그대로 step_permission 에 복사한다. */
    Map<String, MemberPermission> findDefaults(Long stageId);

    /** 참여자를 프로젝트에서 뺄 때 그 사람의 기본값을 전 스테이지에서 지운다. */
    void deleteByProjectIdAndUserId(Long projectId, String userId);
}
