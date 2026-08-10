package com.group3.vitamins.project.stage.infrastructure.adapter;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.port.StepPermissionBulkPort;
import com.group3.vitamins.project.step.application.usecase.StepPermissionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepPermissionBulkAdapter implements StepPermissionBulkPort {

    private final StepPermissionUseCase stepPermissionUseCase;

    @Override
    public int applyToStage(Long stageId, String userId, MemberPermission permission) {
        return stepPermissionUseCase.applyToStage(stageId, userId, permission);
    }
}
