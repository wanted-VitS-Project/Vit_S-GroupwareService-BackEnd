package com.group3.vitamins.department.application.usecase;

import com.group3.vitamins.department.application.command.CreateDepartmentCommand;
import com.group3.vitamins.department.application.command.DeleteDepartmentCommand;
import com.group3.vitamins.department.application.command.RenameDepartmentCommand;
import com.group3.vitamins.department.application.result.DepartmentResult;

/**
 * 부서 생성·수정·삭제 인바운드 포트 (`.ai/api/department.md` §2·§3·§4). 셋 다 ADMIN 전용.
 */
public interface DepartmentCommandUseCase {

    DepartmentResult create(CreateDepartmentCommand command);

    DepartmentResult rename(RenameDepartmentCommand command);

    void delete(DeleteDepartmentCommand command);
}
