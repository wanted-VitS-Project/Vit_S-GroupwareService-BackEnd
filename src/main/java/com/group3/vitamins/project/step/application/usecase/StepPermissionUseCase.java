package com.group3.vitamins.project.step.application.usecase;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.command.RevokeStepPermissionCommand;
import com.group3.vitamins.project.step.application.command.SetStepPermissionCommand;
import com.group3.vitamins.project.step.application.query.StepPermissionListQuery;
import com.group3.vitamins.project.step.application.result.StepPermissionResult;
import com.group3.vitamins.project.step.application.result.StepPermissionSummary;

import java.util.List;

/** 스텝 권한 오버라이드 관리 (STP-010 · STP-011). 세 엔드포인트 모두 프로젝트 EDITOR 소관이다. */
public interface StepPermissionUseCase {

    List<StepPermissionSummary> getPermissions(StepPermissionListQuery query);

    StepPermissionResult setPermission(SetStepPermissionCommand command);

    StepPermissionResult revokePermission(RevokeStepPermissionCommand command);

    /**
     * 스테이지 하위 스텝 전부에 같은 권한을 찍는다 (STG-004). 스테이지 애그리게이트가 호출한다.
     *
     * <p>권한 검사·사원 존재 확인은 하지 않는다 — 호출자가 이미 끝낸 뒤 부른다.
     *
     * @return 적용된 스텝 수
     */
    int applyToStage(Long stageId, String userId, MemberPermission permission);
}
