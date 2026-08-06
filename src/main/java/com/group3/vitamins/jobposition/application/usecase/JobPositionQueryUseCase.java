package com.group3.vitamins.jobposition.application.usecase;

import com.group3.vitamins.jobposition.application.query.JobPositionEmployeesQuery;
import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeesResult;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;

import java.util.List;

public interface JobPositionQueryUseCase {

    List<JobPositionResult> listJobPositions(JobPositionListQuery query);

    /** 직급별 사원 목록 조회 (`.ai/api/job-position.md` §5). 직급이 없으면 {@code POS_NOT_FOUND}. */
    JobPositionEmployeesResult getEmployeesByJobPosition(JobPositionEmployeesQuery query);
}
