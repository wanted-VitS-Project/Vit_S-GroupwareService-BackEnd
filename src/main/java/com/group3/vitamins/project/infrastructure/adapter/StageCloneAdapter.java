package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StageClonePort;
import com.group3.vitamins.project.stage.application.usecase.StageCloneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StageCloneAdapter implements StageClonePort {

    private final StageCloneUseCase stageCloneUseCase;

    @Override
    public Map<Long, Long> cloneStages(Long sourceProjectId, Long targetProjectId) {
        return stageCloneUseCase.cloneToProject(sourceProjectId, targetProjectId);
    }
}
