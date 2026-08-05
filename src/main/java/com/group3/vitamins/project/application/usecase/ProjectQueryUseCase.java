package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.query.ProjectDetailQuery;
import com.group3.vitamins.project.application.result.ProjectDetailResult;

public interface ProjectQueryUseCase {

    ProjectDetailResult getProjectDetail(ProjectDetailQuery query);
}