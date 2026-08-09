package com.group3.vitamins.project.stage.application.port;

import com.group3.vitamins.project.domain.model.MemberPermission;

/**
 * 하위 스텝에 권한을 일괄로 찍는 아웃바운드 포트 (STG-004).
 * step_permission 을 직접 쓰지 않고 스텝 애그리게이트의 인바운드 유스케이스에 위임한다.
 */
public interface StepPermissionBulkPort {

    /** @return 적용된 스텝 수 */
    int applyToStage(Long stageId, String userId, MemberPermission permission);
}
