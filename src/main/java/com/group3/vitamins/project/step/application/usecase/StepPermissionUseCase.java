package com.group3.vitamins.project.step.application.usecase;

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
}
