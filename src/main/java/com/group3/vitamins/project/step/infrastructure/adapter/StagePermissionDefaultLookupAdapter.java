package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.usecase.StagePermissionDefaultUseCase;
import com.group3.vitamins.project.step.application.port.StagePermissionDefaultLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StagePermissionDefaultLookupAdapter implements StagePermissionDefaultLookupPort {

    private final StagePermissionDefaultUseCase stagePermissionDefaultUseCase;

    @Override
    public Map<String, MemberPermission> findDefaults(Long stageId) {
        return stagePermissionDefaultUseCase.findDefaults(stageId);
    }
}
