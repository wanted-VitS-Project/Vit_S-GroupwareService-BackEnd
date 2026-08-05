package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.project.application.port.StepStatLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StepStatLookupAdapter implements StepStatLookupPort {

    private final StepStatQueryMapper stepStatQueryMapper;

    @Override
    public StepStatView countByProjectId(Long projectId) {
        StepStatRow row = stepStatQueryMapper.countByProjectId(projectId);
        return new StepStatView(row.totalCount(), row.doneCount());
    }
}