package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StepPermissionCleanupPort;
import com.group3.vitamins.project.step.application.usecase.StepPermissionCleanupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepPermissionCleanupAdapter implements StepPermissionCleanupPort {

    private final StepPermissionCleanupUseCase stepPermissionCleanupUseCase;

    @Override
    public void deleteByProjectIdAndUserId(Long projectId, String userId) {
        stepPermissionCleanupUseCase.deleteByProjectIdAndUserId(projectId, userId);
    }


}
