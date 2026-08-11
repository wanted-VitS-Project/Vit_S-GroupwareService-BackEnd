package com.group3.vitamins.project.stage.infrastructure.adapter;

import com.group3.vitamins.project.stage.application.port.StepCountLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StepCountLookupAdapter implements StepCountLookupPort {

    private final StepCountQueryMapper stepCountQueryMapper;

    @Override
    public Map<Long, Integer> countByStage(Long projectId, String userId) {
        return stepCountQueryMapper.countByStage(projectId, userId).stream()
                .collect(Collectors.toMap(StepCountRow::stageId, StepCountRow::stepCount));
    }
}