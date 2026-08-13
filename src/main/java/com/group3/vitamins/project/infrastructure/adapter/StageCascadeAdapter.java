package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StageCascadePort;
import com.group3.vitamins.project.stage.application.usecase.StageCascadeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StageCascadeAdapter implements StageCascadePort {

    private final StageCascadeUseCase stageCascadeUseCase;

    @Override
    public int deleteByProjectId(Long projectId) {
        return stageCascadeUseCase.deleteByProjectId(projectId);
    }
}
