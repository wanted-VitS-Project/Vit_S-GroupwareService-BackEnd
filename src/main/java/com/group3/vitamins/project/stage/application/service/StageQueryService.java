package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.stage.application.port.ProjectAccessPort;
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
    private final ProjectAccessPort projectAccessPort;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final StepCountLookupPort stepCountLookupPort;

    @Override
    public List<StageSummary> getStages(StageListQuery query) {
        if (!projectAccessPort.existsProject(query.projectId())) {
            throw new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND);
        }
        projectAccessPolicy.resolvePermission(query.role(),
                projectAccessPort.findPermission(query.projectId(), query.requesterUserId())
                        .orElse(null));

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