package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StepCascadePort;
import com.group3.vitamins.project.step.application.usecase.StepCascadeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepCascadeAdapter implements StepCascadePort {

    private final StepCascadeUseCase stepCascadeUseCase;

    @Override
    public int deleteByProjectId(Long projectId, String requesterUserId) {
        return stepCascadeUseCase.deleteByProjectId(projectId, requesterUserId);
    }
}
