package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.result.ProjectResult;

public interface ProjectCommandUseCase {
    ProjectResult createProject(CreateProjectCommand command);
}