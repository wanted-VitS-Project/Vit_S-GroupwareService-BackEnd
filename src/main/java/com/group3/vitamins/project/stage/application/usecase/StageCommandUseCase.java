package com.group3.vitamins.project.stage.application.usecase;

import com.group3.vitamins.project.stage.application.command.CreateStageCommand;
import com.group3.vitamins.project.stage.application.command.DeleteStageCommand;
import com.group3.vitamins.project.stage.application.command.ReorderStagesCommand;
import com.group3.vitamins.project.stage.application.command.UpdateStageCommand;
import com.group3.vitamins.project.stage.application.result.StageDeleteResult;
import com.group3.vitamins.project.stage.application.result.StageOrderResult;
import com.group3.vitamins.project.stage.application.result.StageResult;

import java.util.List;

public interface StageCommandUseCase {

    StageResult createStage(CreateStageCommand command);

    StageResult updateStage(UpdateStageCommand command);

    List<StageOrderResult> reorderStages(ReorderStagesCommand command);

    StageDeleteResult deleteStage(DeleteStageCommand command);
}
