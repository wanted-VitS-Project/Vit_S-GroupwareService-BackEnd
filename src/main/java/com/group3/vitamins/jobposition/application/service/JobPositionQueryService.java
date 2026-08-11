package com.group3.vitamins.jobposition.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.jobposition.application.policy.JobPositionAdminPolicy;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeQueryPort;
import com.group3.vitamins.jobposition.application.query.JobPositionEmployeesQuery;
import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeRow;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeesResult;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.usecase.JobPositionQueryUseCase;
import com.group3.vitamins.jobposition.domain.exception.JobPositionErrorCode;
import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPositionQueryService implements JobPositionQueryUseCase {

    private final JobPositionRepository jobPositionRepository;
    private final JobPositionEmployeeCountPort jobPositionEmployeeCountPort;
    private final JobPositionEmployeeQueryPort jobPositionEmployeeQueryPort;
    private final JobPositionAdminPolicy jobPositionAdminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public List<JobPositionResult> listJobPositions(JobPositionListQuery query) {
        jobPositionAdminPolicy.assertAdmin(query.role());

        List<JobPosition> jobPositions =
                jobPositionRepository.findAllOrdered(currentCompanyIdProvider.currentCompanyId());
        if (jobPositions.isEmpty()) {
            return List.of();
        }

        // 인원 집계를 한 번에 받아 대조한다 (항목마다 세면 N+1). 인원 0 인 직급은 맵에 없어 0 으로 채운다.
        Map<Long, Integer> countByPosition = jobPositionEmployeeCountPort.countByJobPosition();

        return jobPositions.stream()
                .map(position -> JobPositionResult.of(
                        position,
                        countByPosition.getOrDefault(position.getJobPositionId(), 0)))
                .toList();
    }

    /**
     * 직급별 사원 목록 조회 (`.ai/api/job-position.md` §5).
     *
     * <p>직급 존재를 먼저 확인해 없으면 {@code POS_NOT_FOUND}(404) — 인원 0명(존재하는 직급)과 구분한다.
     * 모집단(시스템·퇴사·삭제 제외)은 포트의 SQL 이 §1 {@code employeeCount} 와 동일하게 거른다.
     * {@code departmentPath}("본사 / 개발팀")는 상위 부서명 + 부서명으로 여기서 조립한다.
     */
    @Override
    public JobPositionEmployeesResult getEmployeesByJobPosition(JobPositionEmployeesQuery query) {
        jobPositionAdminPolicy.assertAdmin(query.role());

        JobPosition jobPosition = jobPositionRepository.findById(
                        query.jobPositionId(), currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(JobPositionErrorCode.POS_NOT_FOUND));

        List<JobPositionEmployeesResult.Employee> employees =
                jobPositionEmployeeQueryPort.findEmployeesByJobPosition(query.jobPositionId()).stream()
                        .map(row -> new JobPositionEmployeesResult.Employee(
                                row.userId(), row.name(),
                                row.departmentName(), departmentPath(row)))
                        .toList();

        return new JobPositionEmployeesResult(
                jobPosition.getJobPositionId(), jobPosition.getName(), employees);
    }

    /** "상위 / 부서" 경로를 조립한다. 부서 미배정이면 null, 최상위 부서면 부서명만. (사원 목록과 동일 규칙) */
    private String departmentPath(JobPositionEmployeeRow row) {
        if (row.departmentName() == null) {
            return null;
        }
        return row.parentDepartmentName() == null
                ? row.departmentName()
                : row.parentDepartmentName() + " / " + row.departmentName();
    }
}
