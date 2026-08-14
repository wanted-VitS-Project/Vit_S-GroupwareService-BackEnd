package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.command.ChangeProjectStatusCommand;
import com.group3.vitamins.project.application.command.CloseProjectCommand;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.command.DeleteProjectCommand;
import com.group3.vitamins.project.application.command.DuplicateProjectCommand;
import com.group3.vitamins.project.application.command.LinkBusinessCategoriesCommand;
import com.group3.vitamins.project.application.command.UnlinkBusinessCategoryCommand;
import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import com.group3.vitamins.project.application.result.ProjectCategoryResult;
import com.group3.vitamins.project.application.result.ProjectCloseResult;
import com.group3.vitamins.project.application.result.ProjectDuplicateResult;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.result.ProjectStatusResult;
import com.group3.vitamins.project.application.result.ProjectUpdateResult;

public interface ProjectCommandUseCase {
    ProjectResult createProject(CreateProjectCommand command);

    /**
     * 원본의 스테이지·스텝·블록 골격만 복제해 새 프로젝트를 만든다 (PRJ-018).
     * 참여자·기간·담당자·블록 내용은 복제하지 않는다.
     */
    ProjectDuplicateResult duplicateProject(DuplicateProjectCommand command);

    ProjectUpdateResult updateProject(UpdateProjectCommand command);

    ProjectStatusResult changeStatus(ChangeProjectStatusCommand command);

    ProjectCloseResult closeProject(CloseProjectCommand command);

    ProjectCategoryResult linkBusinessCategories(LinkBusinessCategoriesCommand command);

    void unlinkBusinessCategory(UnlinkBusinessCategoryCommand command);

    /** 진행 전 + 스텝 0개일 때만 논리 삭제한다. 그 외에는 409 — 종결로 처리해야 한다 (PRJ-014). */
    void deleteProject(DeleteProjectCommand command);
}
