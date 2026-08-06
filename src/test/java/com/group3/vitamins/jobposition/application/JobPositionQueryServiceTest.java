package com.group3.vitamins.jobposition.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.jobposition.application.policy.JobPositionAdminPolicy;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeQueryPort;
import com.group3.vitamins.jobposition.application.query.JobPositionEmployeesQuery;
import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeeRow;
import com.group3.vitamins.jobposition.application.result.JobPositionEmployeesResult;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.service.JobPositionQueryService;
import com.group3.vitamins.jobposition.domain.exception.JobPositionErrorCode;
import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JobPositionQueryService 직급 목록 조회")
class JobPositionQueryServiceTest {

    private JobPositionRepository jobPositionRepository;
    private JobPositionEmployeeCountPort employeeCountPort;
    private JobPositionEmployeeQueryPort employeeQueryPort;
    private JobPositionQueryService queryService;

    @BeforeEach
    void setUp() {
        jobPositionRepository = Mockito.mock(JobPositionRepository.class);
        employeeCountPort = Mockito.mock(JobPositionEmployeeCountPort.class);
        employeeQueryPort = Mockito.mock(JobPositionEmployeeQueryPort.class);
        queryService = new JobPositionQueryService(
                jobPositionRepository, employeeCountPort, employeeQueryPort, new JobPositionAdminPolicy());
    }

    private JobPosition position(Long id, String name, int sortOrder) {
        return JobPosition.restore(id, name, sortOrder);
    }

    @Test
    @DisplayName("각 직급에 사용 인원을 붙여 반환하고, 집계에 없는 직급은 0 으로 채운다")
    void mapsEmployeeCountAndDefaultsZero() {
        when(jobPositionRepository.findAllOrdered()).thenReturn(List.of(
                position(1L, "사원", 1),
                position(2L, "대리", 2),
                position(3L, "과장", 3)   // 집계 맵에 없음 → 0
        ));
        when(employeeCountPort.countByJobPosition()).thenReturn(Map.of(1L, 14, 2L, 6));

        List<JobPositionResult> result = queryService.listJobPositions(new JobPositionListQuery("ADMIN"));

        assertThat(result).extracting(JobPositionResult::name)
                .containsExactly("사원", "대리", "과장");
        assertThat(result).extracting(JobPositionResult::employeeCount)
                .containsExactly(14, 6, 0);
    }

    @Test
    @DisplayName("직급이 하나도 없으면 빈 배열이고 집계는 조회하지 않는다")
    void emptyWhenNoPositions() {
        when(jobPositionRepository.findAllOrdered()).thenReturn(List.of());

        assertThat(queryService.listJobPositions(new JobPositionListQuery("ADMIN"))).isEmpty();
        verify(employeeCountPort, never()).countByJobPosition();
    }

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 조회 이전에 막는다")
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> queryService.listJobPositions(new JobPositionListQuery("MASTER")))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        verify(jobPositionRepository, never()).findAllOrdered();
    }

    // ===== §5 직급별 사원 목록 =====

    @Test
    @DisplayName("직급에 속한 사원을 반환하고 departmentPath 를 상위/부서로 조립한다")
    void listsEmployeesWithDepartmentPath() {
        when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(position(1L, "사원", 1)));
        when(employeeQueryPort.findEmployeesByJobPosition(1L)).thenReturn(List.of(
                new JobPositionEmployeeRow("EMP001", "김철수", "개발팀", "본사"),   // 하위 부서 → "본사 / 개발팀"
                new JobPositionEmployeeRow("EMP002", "박영수", "본사", null),      // 최상위 부서 → "본사"
                new JobPositionEmployeeRow("EMP014", "이영희", null, null)         // 부서 미배정 → null
        ));

        JobPositionEmployeesResult result =
                queryService.getEmployeesByJobPosition(new JobPositionEmployeesQuery(1L, "ADMIN"));

        assertThat(result.jobPositionId()).isEqualTo(1L);
        assertThat(result.jobPositionName()).isEqualTo("사원");
        assertThat(result.content()).extracting(JobPositionEmployeesResult.Employee::departmentPath)
                .containsExactly("본사 / 개발팀", "본사", null);
        assertThat(result.content()).extracting(JobPositionEmployeesResult.Employee::userId)
                .containsExactly("EMP001", "EMP002", "EMP014");
    }

    @Test
    @DisplayName("존재하는 직급이지만 인원이 0명이면 빈 배열")
    void emptyWhenNoEmployees() {
        when(jobPositionRepository.findById(2L)).thenReturn(Optional.of(position(2L, "대리", 2)));
        when(employeeQueryPort.findEmployeesByJobPosition(2L)).thenReturn(List.of());

        JobPositionEmployeesResult result =
                queryService.getEmployeesByJobPosition(new JobPositionEmployeesQuery(2L, "ADMIN"));

        assertThat(result.content()).isEmpty();
        assertThat(result.jobPositionName()).isEqualTo("대리");
    }

    @Test
    @DisplayName("직급이 없으면 POS_NOT_FOUND — 사원 조회 이전에 막는다")
    void rejectsMissingPosition() {
        when(jobPositionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getEmployeesByJobPosition(new JobPositionEmployeesQuery(99L, "ADMIN")))
                .satisfies(hasCode(JobPositionErrorCode.POS_NOT_FOUND));
        verify(employeeQueryPort, never()).findEmployeesByJobPosition(anyLong());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 직급 조회 이전에 막는다")
    void employeesRejectsNonAdmin() {
        assertThatThrownBy(() -> queryService.getEmployeesByJobPosition(new JobPositionEmployeesQuery(1L, "MASTER")))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        verify(jobPositionRepository, never()).findById(anyLong());
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
