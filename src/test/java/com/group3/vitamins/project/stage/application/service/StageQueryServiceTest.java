package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.port.ProjectAccessPort;
import com.group3.vitamins.project.stage.application.port.StepCountLookupPort;
import com.group3.vitamins.project.stage.application.query.StageListQuery;
import com.group3.vitamins.project.stage.application.result.StageSummary;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("StageQueryService 스테이지 목록 조회")
class StageQueryServiceTest {

    private static final Long PROJECT_ID = 12L;
    private static final String REQUESTER_ID = "E2024001";

    private StageRepository stageRepository;
    private ProjectAccessPort projectAccessPort;
    private ProjectAccessPolicy projectAccessPolicy;
    private StepCountLookupPort stepCountLookupPort;
    private StageQueryService stageQueryService;

    @BeforeEach
    void setUp() {
        stageRepository = Mockito.mock(StageRepository.class);
        projectAccessPort = Mockito.mock(ProjectAccessPort.class);
        projectAccessPolicy = new ProjectAccessPolicy();
        stepCountLookupPort = Mockito.mock(StepCountLookupPort.class);
        stageQueryService = new StageQueryService(stageRepository, projectAccessPort,
                projectAccessPolicy, stepCountLookupPort);
    }

    private StageListQuery query(String role) {
        return new StageListQuery(PROJECT_ID, REQUESTER_ID, role);
    }

    private Stage stage(Long stageId, String name, int sortOrder) {
        return Stage.restore(stageId, PROJECT_ID, name, sortOrder, LocalDateTime.of(2026, 8, 1, 10, 0), null);
    }

    private void givenAccessibleProject(MemberPermission permission) {
        when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
        when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID)).thenReturn(Optional.of(permission));
    }

    private Consumer<Throwable> hasCode(ErrorCode expected) {
        return thrown -> {
            assertThat(thrown).isInstanceOf(DomainException.class);
            assertThat(((DomainException) thrown).getErrorCode()).isEqualTo(expected);
        };
    }

    @Nested
    @DisplayName("프로젝트 접근 검증")
    class ProjectAccessValidation {

        @Test
        @DisplayName("프로젝트가 없으면 PROJECT_NOT_FOUND")
        void rejectsMissingProject() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(false);

            assertThatThrownBy(() -> stageQueryService.getStages(query("MEMBER")))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
        }

        @Test
        @DisplayName("참여자가 아니면 PROJECT_ACCESS_DENIED")
        void rejectsNonMember() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
            when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stageQueryService.getStages(query("MEMBER")))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_ACCESS_DENIED));
        }

        @Test
        @DisplayName("VIEWER 권한이면 조회는 통과한다")
        void allowsViewer() {
            givenAccessibleProject(MemberPermission.VIEWER);
            when(stepCountLookupPort.countByStage(PROJECT_ID, REQUESTER_ID)).thenReturn(Map.of());
            when(stageRepository.findAllByProjectId(PROJECT_ID)).thenReturn(List.of());

            List<StageSummary> result = stageQueryService.getStages(query("MEMBER"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("MASTER 는 참여자가 아니어도 통과한다")
        void allowsGlobalAdminWithoutMembership() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
            when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID)).thenReturn(Optional.empty());
            when(stepCountLookupPort.countByStage(PROJECT_ID, REQUESTER_ID)).thenReturn(Map.of());
            when(stageRepository.findAllByProjectId(PROJECT_ID)).thenReturn(List.of());

            List<StageSummary> result = stageQueryService.getStages(query("MASTER"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("목록 조회 결과")
    class ListResult {

        @Test
        @DisplayName("스테이지가 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNoStages() {
            givenAccessibleProject(MemberPermission.EDITOR);
            when(stepCountLookupPort.countByStage(PROJECT_ID, REQUESTER_ID)).thenReturn(Map.of());
            when(stageRepository.findAllByProjectId(PROJECT_ID)).thenReturn(List.of());

            List<StageSummary> result = stageQueryService.getStages(query("MEMBER"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("스텝 수가 있으면 채우고, 없는 스테이지는 0으로 채운다")
        void fillsStepCountsWithDefaultZero() {
            givenAccessibleProject(MemberPermission.EDITOR);
            when(stepCountLookupPort.countByStage(PROJECT_ID, REQUESTER_ID)).thenReturn(Map.of(7L, 3));
            when(stageRepository.findAllByProjectId(PROJECT_ID))
                    .thenReturn(List.of(stage(7L, "제안", 1), stage(8L, "계약", 2)));

            List<StageSummary> result = stageQueryService.getStages(query("MEMBER"));

            assertThat(result).containsExactly(
                    new StageSummary(7L, "제안", 1, 3),
                    new StageSummary(8L, "계약", 2, 0));
        }
    }
}
