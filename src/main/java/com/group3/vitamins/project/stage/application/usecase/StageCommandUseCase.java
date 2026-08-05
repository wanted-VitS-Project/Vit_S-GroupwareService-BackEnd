package com.group3.vitamins.project.stage.application.usecase;

import com.group3.vitamins.project.stage.application.command.CreateStageCommand;
import com.group3.vitamins.project.stage.application.result.StageResult;

public interface StageCommandUseCase {

    StageResult createStage(CreateStageCommand command);
}