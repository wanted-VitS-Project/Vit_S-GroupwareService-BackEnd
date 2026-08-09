package com.group3.vitamins.project.step.application.usecase;

import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.application.result.StepUpdateResult;

public interface StepCommandUseCase {

    StepResult createStep(CreateStepCommand command);

    StepUpdateResult updateStep(UpdateStepCommand command);
}
