package com.group3.vitamins.project.stage.infrastructure.adapter;

import com.group3.vitamins.project.stage.application.port.StepRelocationPort;
import com.group3.vitamins.project.step.application.usecase.StepRelocationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepRelocationAdapter implements StepRelocationPort {

    private final StepRelocationUseCase stepRelocationUseCase;

    @Override
    public int relocateByStage(Long fromStageId, Long toStageId) {
        return stepRelocationUseCase.relocateByStage(fromStageId, toStageId);
    }
}
