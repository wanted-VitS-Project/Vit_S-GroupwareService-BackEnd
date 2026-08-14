package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StepClonePort;
import com.group3.vitamins.project.step.application.usecase.StepCloneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StepCloneAdapter implements StepClonePort {

    private final StepCloneUseCase stepCloneUseCase;

    @Override
    public Map<Long, Long> cloneSteps(Long sourceProjectId, Long targetProjectId,
                                      Map<Long, Long> stageIdMap) {
        return stepCloneUseCase.cloneToProject(sourceProjectId, targetProjectId, stageIdMap);
    }
}
