package com.group3.vitamins.jobposition.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.jobposition.application.policy.JobPositionAdminPolicy;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.service.JobPositionQueryService;
import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JobPositionQueryService 직급 목록 조회")
class JobPositionQueryServiceTest {

    private JobPositionRepository jobPositionRepository;
    private JobPositionEmployeeCountPort employeeCountPort;
    private JobPositionQueryService queryService;

    @BeforeEach
    void setUp() {
        jobPositionRepository = Mockito.mock(JobPositionRepository.class);
        employeeCountPort = Mockito.mock(JobPositionEmployeeCountPort.class);
        queryService = new JobPositionQueryService(
                jobPositionRepository, employeeCountPort, new JobPositionAdminPolicy());
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

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
