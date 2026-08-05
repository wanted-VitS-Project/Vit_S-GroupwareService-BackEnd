package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.stage.application.port.StepCountLookupPort;
import com.group3.vitamins.project.stage.application.query.StageListQuery;
import com.group3.vitamins.project.stage.application.result.StageSummary;
import com.group3.vitamins.project.stage.application.usecase.StageQueryUseCase;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageQueryService implements StageQueryUseCase {

    private final StageRepository stageRepository;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final StepCountLookupPort stepCountLookupPort;

    @Override
    public List<StageSummary> getStages(StageListQuery query) {
        projectAccessUseCase.requireAccess(
                query.projectId(), query.requesterUserId(), query.role());

        Map<Long, Integer> stepCounts =
                stepCountLookupPort.countByStage(query.projectId(), query.requesterUserId());

        return stageRepository.findAllByProjectId(query.projectId()).stream()
                .map(stage -> new StageSummary(
                        stage.getStageId(),
                        stage.getName(),
                        stage.getSortOrder(),
                        stepCounts.getOrDefault(stage.getStageId(), 0)))
                .toList();
    }
}