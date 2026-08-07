package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.query.ProjectDetailQuery;
import com.group3.vitamins.project.application.query.ProjectListQuery;
import com.group3.vitamins.project.application.query.ProjectProgressQuery;
import com.group3.vitamins.project.application.result.ProjectDetailResult;
import com.group3.vitamins.project.application.result.ProjectPageResult;
import com.group3.vitamins.project.application.result.ProjectProgressResult;

public interface ProjectQueryUseCase {

    ProjectDetailResult getProjectDetail(ProjectDetailQuery query);

    ProjectPageResult getProjects(ProjectListQuery query);

    ProjectProgressResult getProjectProgress(ProjectProgressQuery query);
}