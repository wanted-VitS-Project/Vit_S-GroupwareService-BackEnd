package com.group3.vitamins.project.stage.application.usecase;

import com.group3.vitamins.project.stage.application.command.ApplyStagePermissionCommand;
import com.group3.vitamins.project.stage.application.result.StageStepPermissionResult;

/** 하위 스텝 권한 일괄 적용 (STG-004). 프로젝트 EDITOR 소관이다. */
public interface StageStepPermissionUseCase {

    StageStepPermissionResult apply(ApplyStagePermissionCommand command);
}
