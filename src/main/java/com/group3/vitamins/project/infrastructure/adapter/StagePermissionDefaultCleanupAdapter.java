package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StagePermissionDefaultCleanupPort;
import com.group3.vitamins.project.stage.application.usecase.StagePermissionDefaultUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StagePermissionDefaultCleanupAdapter implements StagePermissionDefaultCleanupPort {

    private final StagePermissionDefaultUseCase stagePermissionDefaultUseCase;

    @Override
    public void deleteByProjectIdAndUserId(Long projectId, String userId) {
        stagePermissionDefaultUseCase.deleteByProjectIdAndUserId(projectId, userId);
    }
}
