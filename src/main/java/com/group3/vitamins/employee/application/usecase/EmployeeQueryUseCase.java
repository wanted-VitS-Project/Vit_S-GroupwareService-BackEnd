package com.group3.vitamins.employee.application.usecase;

import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.result.EmployeeSearchRow;

import java.util.List;

public interface EmployeeQueryUseCase {

    /** 사원 후보 검색 — 이름 또는 부서 (결재선·참여자 지정용, `.ai/api/employee.md` §9). */
    List<EmployeeSearchRow> search(EmployeeSearchQuery query);
}
