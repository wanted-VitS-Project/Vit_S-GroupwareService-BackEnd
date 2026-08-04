package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.policy.StepAccessPolicy;
import com.group3.vitamins.project.step.application.port.IssueStatLookupPort;
import com.group3.vitamins.project.step.application.query.StepDetailQuery;
import com.group3.vitamins.project.step.application.query.StepListQuery;
import com.group3.vitamins.project.step.application.result.StepDetailResult;
import com.group3.vitamins.project.step.application.result.StepPerson;
import com.group3.vitamins.project.step.application.result.StepSummary;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("StepQueryService 스텝 조회")
class StepQueryServiceTest {

    private static final Long PROJECT_ID = 12L;
    private static final Long STAGE_ID = 7L;
    private static final Long STEP_ID = 10L;
    private static final Long OTHER_STEP_ID = 11L;
    private static final String REQUESTER_ID = "E2024001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 10, 0);

    private StepRepository stepRepository;
    private StepPermissionRepository stepPermissionRepository;
    private IssueStatLookupPort issueStatLookupPort;
    private EmployeeLookupPort employeeLookupPort;
    private ProjectAccessUseCase projectAccessUseCase;
    private StepAccessPolicy stepAccessPolicy;
    private StepQueryService stepQueryService;

    @BeforeEach
    void setUp() {
        stepRepository = Mockito.mock(StepRepository.class);
        stepPermissionRepository = Mockito.mock(StepPermissionRepository.class);
        issueStatLookupPort = Mockito.mock(IssueStatLookupPort.class);
        employeeLookupPort = Mockito.mock(EmployeeLookupPort.class);
        projectAccessUseCase = Mockito.mock(ProjectAccessUseCase.class);
        stepAccessPolicy = new StepAccessPolicy();
        stepQueryService = new StepQueryService(stepRepository, stepPermissionRepository,
                issueStatLookupPort, employeeLookupPort, projectAccessUseCase, stepAccessPolicy);
    }

    private StepDetailQuery detailQuery() {
        return new StepDetailQuery(STEP_ID, REQUESTER_ID, ROLE);
    }

    private StepDetailQuery detailQuery(String role) {
        return new StepDetailQuery(STEP_ID, REQUESTER_ID, role);
    }

    private StepListQuery listQuery() {
        return new StepListQuery(PROJECT_ID, null, null, REQUESTER_ID, ROLE);
    }

    private StepListQuery listQuery(String role) {
        return new StepListQuery(PROJECT_ID, null, null, REQUESTER_ID, role);
    }

    private Step step(Long stepId, String ownerUserId, String completedBy, LocalDateTime completedAt) {
        return Step.restore(stepId, PROJECT_ID, STAGE_ID, "제안서 작성", 1,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), ownerUserId,
                StepStatus.IN_PROGRESS, completedAt, completedBy, NOW, NOW, null);
    }

    private Step step(Long stepId) {
        return step(stepId, REQUESTER_ID, null, null);
    }

    private Consumer<Throwable> hasCode(ErrorCode expected) {
        return thrown -> {
            assertThat(thrown).isInstanceOf(DomainException.class);
            assertThat(((DomainException) thrown).getErrorCode()).isEqualTo(expected);
        };
    }

    @Nested
    @DisplayName("스텝 상세 조회")
    class GetStepDetail {

        @Test
        @DisplayName("스텝이 없으면 STEP_NOT_FOUND")
        void rejectsWhenStepNotFound() {
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stepQueryService.getStepDetail(detailQuery()))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(hasCode(StepErrorCode.STEP_NOT_FOUND));
            verify(projectAccessUseCase, never()).resolvePermission(any(), any(), any());
        }

        @Test
        @DisplayName("프로젝트 권한이 NONE 이고 오버라이드도 없으면 STEP_ACCESS_DENIED")
        void rejectsWhenNoAccess() {
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step(STEP_ID)));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.NONE);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> stepQueryService.getStepDetail(detailQuery()))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(StepErrorCode.STEP_ACCESS_DENIED));
        }

        @Test
        @DisplayName("오버라이드가 NONE 이면 프로젝트 권한이 EDITOR 여도 STEP_ACCESS_DENIED")
        void overrideCanBlockDespiteProjectEditor() {
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step(STEP_ID)));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(MemberPermission.NONE));

            assertThatThrownBy(() -> stepQueryService.getStepDetail(detailQuery()))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(StepErrorCode.STEP_ACCESS_DENIED));
        }

        @Test
        @DisplayName("오버라이드가 EDITOR 면 프로젝트 권한이 VIEWER 여도 myPermission 은 EDITOR")
        void overrideCanGrantDespiteProjectViewer() {
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step(STEP_ID)));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.VIEWER);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(MemberPermission.EDITOR));
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of(REQUESTER_ID)))
                    .thenReturn(Map.of());

            StepDetailResult result = stepQueryService.getStepDetail(detailQuery());

            assertThat(result.myPermission()).isEqualTo("EDITOR");
        }

        @Test
        @DisplayName("MASTER 역할이면 오버라이드가 NONE 이어도 EDITOR 로 접근한다")
        void globalAdminBypassesOverride() {
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step(STEP_ID)));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, "MASTER"))
                    .thenReturn(MemberPermission.NONE);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(MemberPermission.NONE));
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of(REQUESTER_ID)))
                    .thenReturn(Map.of());

            StepDetailResult result = stepQueryService.getStepDetail(detailQuery("MASTER"));

            assertThat(result.myPermission()).isEqualTo("EDITOR");
        }

        @Test
        @DisplayName("이슈 통계·책임자·완료자 이름을 채워 응답한다")
        void mapsFullDetail() {
            Step step = step(STEP_ID, "E2024001", "E2024003", NOW);
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID)))
                    .thenReturn(Map.of(STEP_ID, new IssueStatLookupPort.IssueStatView(5, 2, 2)));
            when(employeeLookupPort.findNamesByUserIds(Set.of("E2024001", "E2024003")))
                    .thenReturn(Map.of("E2024001", "김용준", "E2024003", "이서연"));

            StepDetailResult result = stepQueryService.getStepDetail(detailQuery());

            assertThat(result.stepId()).isEqualTo(STEP_ID);
            assertThat(result.projectId()).isEqualTo(PROJECT_ID);
            assertThat(result.stageId()).isEqualTo(STAGE_ID);
            assertThat(result.name()).isEqualTo("제안서 작성");
            assertThat(result.status()).isEqualTo("IN_PROGRESS");
            assertThat(result.owner()).isEqualTo(new StepPerson("E2024001", "김용준"));
            assertThat(result.totalIssueCount()).isEqualTo(5);
            assertThat(result.doneIssueCount()).isEqualTo(2);
            assertThat(result.inProgressIssueCount()).isEqualTo(2);
            assertThat(result.progressRate()).isEqualTo(40);
            assertThat(result.completedBy()).isEqualTo(new StepPerson("E2024003", "이서연"));
            assertThat(result.completedAt()).isEqualTo(NOW);
            assertThat(result.myPermission()).isEqualTo("EDITOR");
        }

        @Test
        @DisplayName("이슈가 0개면 progressRate 는 null 이다 (INV-04)")
        void progressRateNullWhenNoIssues() {
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step(STEP_ID)));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of(REQUESTER_ID)))
                    .thenReturn(Map.of());

            StepDetailResult result = stepQueryService.getStepDetail(detailQuery());

            assertThat(result.totalIssueCount()).isZero();
            assertThat(result.progressRate()).isNull();
        }

        @Test
        @DisplayName("책임자·완료자가 모두 없으면 owner·completedBy 는 null 이고 빈 사번 집합으로 조회한다")
        void ownerAndCompletedByNullWhenAbsent() {
            Step step = step(STEP_ID, null, null, null);
            when(stepRepository.findById(STEP_ID)).thenReturn(Optional.of(step));
            when(projectAccessUseCase.resolvePermission(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepPermissionRepository.findOverride(STEP_ID, REQUESTER_ID))
                    .thenReturn(Optional.empty());
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of())).thenReturn(Map.of());

            StepDetailResult result = stepQueryService.getStepDetail(detailQuery());

            assertThat(result.owner()).isNull();
            assertThat(result.completedBy()).isNull();
            verify(employeeLookupPort).findNamesByUserIds(Set.of());
        }
    }

    @Nested
    @DisplayName("스텝 목록 조회")
    class GetSteps {

        @Test
        @DisplayName("프로젝트 접근 권한이 없으면 PROJECT_ACCESS_DENIED 가 그대로 전파된다")
        void propagatesProjectAccessDenied() {
            doThrow(new ForbiddenException(ProjectErrorCode.PROJECT_ACCESS_DENIED))
                    .when(projectAccessUseCase).requireAccess(PROJECT_ID, REQUESTER_ID, ROLE);

            assertThatThrownBy(() -> stepQueryService.getSteps(listQuery()))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_ACCESS_DENIED));
            verify(stepRepository, never()).search(any(), any(), any());
        }

        @Test
        @DisplayName("프로젝트가 없으면 PROJECT_NOT_FOUND 가 그대로 전파된다")
        void propagatesProjectNotFound() {
            doThrow(new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND))
                    .when(projectAccessUseCase).requireAccess(PROJECT_ID, REQUESTER_ID, ROLE);

            assertThatThrownBy(() -> stepQueryService.getSteps(listQuery()))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
        }

        @Test
        @DisplayName("스텝이 하나도 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoSteps() {
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepRepository.search(PROJECT_ID, null, null)).thenReturn(List.of());

            List<StepSummary> result = stepQueryService.getSteps(listQuery());

            assertThat(result).isEmpty();
            verify(issueStatLookupPort, never()).countByStepIds(any());
        }

        @Test
        @DisplayName("모든 스텝의 유효 권한이 NONE 이면 빈 목록을 반환한다 (STP-010)")
        void returnsEmptyWhenAllStepsInaccessible() {
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepRepository.search(PROJECT_ID, null, null)).thenReturn(List.of(step(STEP_ID)));
            when(stepPermissionRepository.findOverrides(List.of(STEP_ID), REQUESTER_ID))
                    .thenReturn(Map.of(STEP_ID, MemberPermission.NONE));

            List<StepSummary> result = stepQueryService.getSteps(listQuery());

            assertThat(result).isEmpty();
            verify(issueStatLookupPort, never()).countByStepIds(any());
            verify(employeeLookupPort, never()).findNamesByUserIds(any());
        }

        @Test
        @DisplayName("일부 스텝만 NONE 이면 나머지만 반환한다")
        void filtersOutInaccessibleSteps() {
            Step visible = step(STEP_ID);
            Step blocked = step(OTHER_STEP_ID);
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.VIEWER);
            when(stepRepository.search(PROJECT_ID, null, null))
                    .thenReturn(List.of(visible, blocked));
            when(stepPermissionRepository.findOverrides(List.of(STEP_ID, OTHER_STEP_ID), REQUESTER_ID))
                    .thenReturn(Map.of(OTHER_STEP_ID, MemberPermission.NONE));
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of(REQUESTER_ID))).thenReturn(Map.of());

            List<StepSummary> result = stepQueryService.getSteps(listQuery());

            assertThat(result).extracting(StepSummary::stepId).containsExactly(STEP_ID);
        }

        @Test
        @DisplayName("MASTER 역할이면 오버라이드가 NONE 이어도 모든 스텝을 본다")
        void globalAdminSeesEverything() {
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, "MASTER"))
                    .thenReturn(MemberPermission.NONE);
            when(stepRepository.search(PROJECT_ID, null, null)).thenReturn(List.of(step(STEP_ID)));
            when(stepPermissionRepository.findOverrides(List.of(STEP_ID), REQUESTER_ID))
                    .thenReturn(Map.of(STEP_ID, MemberPermission.NONE));
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of(REQUESTER_ID))).thenReturn(Map.of());

            List<StepSummary> result = stepQueryService.getSteps(listQuery("MASTER"));

            assertThat(result).extracting(StepSummary::myPermission).containsExactly("EDITOR");
        }

        @Test
        @DisplayName("stageId·status 파라미터를 그대로 repository 에 전달한다")
        void forwardsFilterParameters() {
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepRepository.search(PROJECT_ID, STAGE_ID, StepStatus.IN_PROGRESS))
                    .thenReturn(List.of());

            stepQueryService.getSteps(
                    new StepListQuery(PROJECT_ID, STAGE_ID, StepStatus.IN_PROGRESS, REQUESTER_ID, ROLE));

            verify(stepRepository).search(PROJECT_ID, STAGE_ID, StepStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("책임자 사번을 모아 한 번에 이름을 조회한다 (N+1 방지)")
        void batchesOwnerNameLookup() {
            Step s1 = step(STEP_ID, "E2024001", null, null);
            Step s2 = step(OTHER_STEP_ID, "E2024002", null, null);
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepRepository.search(PROJECT_ID, null, null)).thenReturn(List.of(s1, s2));
            when(stepPermissionRepository.findOverrides(List.of(STEP_ID, OTHER_STEP_ID), REQUESTER_ID))
                    .thenReturn(Map.of());
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID, OTHER_STEP_ID)))
                    .thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of("E2024001", "E2024002")))
                    .thenReturn(Map.of("E2024001", "김용준", "E2024002", "박서준"));

            stepQueryService.getSteps(listQuery());

            verify(employeeLookupPort, times(1)).findNamesByUserIds(any());
            verify(stepPermissionRepository, times(1)).findOverrides(anyList(), eq(REQUESTER_ID));
        }

        @Test
        @DisplayName("정상 요청이면 스텝 요약 필드를 그대로 매핑한다")
        void mapsSummaryFields() {
            Step step = step(STEP_ID, "E2024001", null, null);
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepRepository.search(PROJECT_ID, null, null)).thenReturn(List.of(step));
            when(stepPermissionRepository.findOverrides(List.of(STEP_ID), REQUESTER_ID))
                    .thenReturn(Map.of());
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID)))
                    .thenReturn(Map.of(STEP_ID, new IssueStatLookupPort.IssueStatView(5, 2, 2)));
            when(employeeLookupPort.findNamesByUserIds(Set.of("E2024001")))
                    .thenReturn(Map.of("E2024001", "김용준"));

            List<StepSummary> result = stepQueryService.getSteps(listQuery());

            assertThat(result).hasSize(1);
            StepSummary summary = result.get(0);
            assertThat(summary.stepId()).isEqualTo(STEP_ID);
            assertThat(summary.stageId()).isEqualTo(STAGE_ID);
            assertThat(summary.name()).isEqualTo("제안서 작성");
            assertThat(summary.status()).isEqualTo("IN_PROGRESS");
            assertThat(summary.sortOrder()).isEqualTo(1);
            assertThat(summary.owner()).isEqualTo(new StepPerson("E2024001", "김용준"));
            assertThat(summary.totalIssueCount()).isEqualTo(5);
            assertThat(summary.doneIssueCount()).isEqualTo(2);
            assertThat(summary.inProgressIssueCount()).isEqualTo(2);
            assertThat(summary.progressRate()).isEqualTo(40);
            assertThat(summary.myPermission()).isEqualTo("EDITOR");
        }

        @Test
        @DisplayName("이슈가 0개인 스텝은 progressRate 가 null 이다")
        void progressRateNullWhenNoIssues() {
            when(projectAccessUseCase.requireAccess(PROJECT_ID, REQUESTER_ID, ROLE))
                    .thenReturn(MemberPermission.EDITOR);
            when(stepRepository.search(PROJECT_ID, null, null)).thenReturn(List.of(step(STEP_ID)));
            when(stepPermissionRepository.findOverrides(List.of(STEP_ID), REQUESTER_ID))
                    .thenReturn(Map.of());
            when(issueStatLookupPort.countByStepIds(List.of(STEP_ID))).thenReturn(Map.of());
            when(employeeLookupPort.findNamesByUserIds(Set.of(REQUESTER_ID))).thenReturn(Map.of());

            List<StepSummary> result = stepQueryService.getSteps(listQuery());

            assertThat(result.get(0).progressRate()).isNull();
        }
    }
}
