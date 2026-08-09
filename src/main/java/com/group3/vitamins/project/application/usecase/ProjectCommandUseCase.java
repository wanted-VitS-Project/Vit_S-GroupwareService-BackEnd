package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.command.ChangeProjectStatusCommand;
import com.group3.vitamins.project.application.command.CloseProjectCommand;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import com.group3.vitamins.project.application.result.ProjectCloseResult;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.result.ProjectStatusResult;
import com.group3.vitamins.project.application.result.ProjectUpdateResult;

public interface ProjectCommandUseCase {
    ProjectResult createProject(CreateProjectCommand command);

    ProjectUpdateResult updateProject(UpdateProjectCommand command);

    ProjectStatusResult changeStatus(ChangeProjectStatusCommand command);

    ProjectCloseResult closeProject(CloseProjectCommand command);
}
