package com.group3.vitamins.employee.application.usecase;

import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.result.EmployeeSearchRow;

import java.util.List;

public interface EmployeeQueryUseCase {

    /** 사원 이름 검색 (결재선 지정용, `.ai/api/employee.md` §9). */
    List<EmployeeSearchRow> searchByName(EmployeeSearchQuery query);
}
