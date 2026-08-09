package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.stage.application.command.CreateStageCommand;
import com.group3.vitamins.project.stage.application.result.StageResult;
import com.group3.vitamins.project.stage.application.usecase.StageCommandUseCase;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StageCommandService implements StageCommandUseCase {

    private static final int FIRST_SORT_ORDER = 1;

    private final StageRepository stageRepository;
    private final ProjectAccessUseCase projectAccessUseCase;

    @Override
    public StageResult createStage(CreateStageCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        int sortOrder = command.sortOrder() != null
                ? command.sortOrder()
                : nextSortOrder(command.projectId());

        Stage saved = stageRepository.save(Stage.create(
                command.projectId(), command.name(), sortOrder, LocalDateTime.now()));

        return new StageResult(saved.getStageId(), saved.getProjectId(),
                saved.getName(), saved.getSortOrder());
    }

    /** sortOrder 미지정 시 max+1 을 쓴다. 스테이지가 없으면 1 부터 시작한다. */
    private int nextSortOrder(Long projectId) {
        return stageRepository.findMaxSortOrder(projectId)
                .map(max -> max + 1)
                .orElse(FIRST_SORT_ORDER);
    }
}