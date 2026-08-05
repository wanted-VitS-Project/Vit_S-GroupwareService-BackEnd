package com.group3.vitamins.project.step.application.usecase;

import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import com.group3.vitamins.project.step.application.result.StepResult;

public interface StepCommandUseCase {

    StepResult createStep(CreateStepCommand command);
}