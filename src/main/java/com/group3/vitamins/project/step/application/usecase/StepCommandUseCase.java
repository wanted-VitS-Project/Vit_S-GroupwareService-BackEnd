package com.group3.vitamins.project.step.application.usecase;

import com.group3.vitamins.project.step.application.command.ChangeStepStatusCommand;
import com.group3.vitamins.project.step.application.command.CompleteStepCommand;
import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import com.group3.vitamins.project.step.application.command.ReorderStepsCommand;
import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import com.group3.vitamins.project.step.application.result.StepCompleteResult;
import com.group3.vitamins.project.step.application.result.StepOrderResult;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.application.result.StepStatusResult;
import com.group3.vitamins.project.step.application.result.StepUpdateResult;

import java.util.List;

public interface StepCommandUseCase {

    StepResult createStep(CreateStepCommand command);

    StepUpdateResult updateStep(UpdateStepCommand command);

    List<StepOrderResult> reorderSteps(ReorderStepsCommand command);

    StepStatusResult changeStatus(ChangeStepStatusCommand command);

    StepCompleteResult completeStep(CompleteStepCommand command);
}
