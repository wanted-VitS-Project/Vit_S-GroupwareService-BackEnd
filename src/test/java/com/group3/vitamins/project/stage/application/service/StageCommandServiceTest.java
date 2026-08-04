package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.command.CreateStageCommand;
import com.group3.vitamins.project.stage.application.port.ProjectAccessPort;
import com.group3.vitamins.project.stage.application.result.StageResult;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("StageCommandService 스테이지 생성")
class StageCommandServiceTest {

    private static final Long PROJECT_ID = 12L;
    private static final Long STAGE_ID = 7L;
    private static final String REQUESTER_ID = "E2024001";
    private static final String VALID_NAME = "제안";

    private StageRepository stageRepository;
    private ProjectAccessPort projectAccessPort;
    private ProjectAccessPolicy projectAccessPolicy;
    private StageCommandService stageCommandService;

    @BeforeEach
    void setUp() {
        stageRepository = Mockito.mock(StageRepository.class);
        projectAccessPort = Mockito.mock(ProjectAccessPort.class);
        projectAccessPolicy = new ProjectAccessPolicy();
        stageCommandService = new StageCommandService(stageRepository, projectAccessPort, projectAccessPolicy);
    }

    private CreateStageCommand command(String name, Integer sortOrder, String role) {
        return new CreateStageCommand(PROJECT_ID, name, sortOrder, REQUESTER_ID, role);
    }

    private Stage savedStage(String name, int sortOrder) {
        return Stage.restore(STAGE_ID, PROJECT_ID, name, sortOrder, LocalDateTime.of(2026, 8, 1, 10, 0), null);
    }

    /** 프로젝트가 존재하고 요청자가 EDITOR 참여자인 상태를 세팅한다. */
    private void givenEditableProject() {
        when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
        when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID))
                .thenReturn(Optional.of(MemberPermission.EDITOR));
    }

    private Consumer<Throwable> hasCode(ErrorCode expected) {
        return thrown -> {
            assertThat(thrown).isInstanceOf(DomainException.class);
            assertThat(((DomainException) thrown).getErrorCode()).isEqualTo(expected);
        };
    }

    @Nested
    @DisplayName("스테이지명 검증")
    class NameValidation {

        @Test
        @DisplayName("이름이 null 이면 STAGE_NAME_REQUIRED")
        void rejectsNullName() {
            assertThatThrownBy(() -> stageCommandService.createStage(command(null, null, "MEMBER")))
                    .satisfies(hasCode(StageErrorCode.STAGE_NAME_REQUIRED));
            verify(projectAccessPort, never()).existsProject(anyLong());
        }

        @Test
        @DisplayName("이름이 공백이면 STAGE_NAME_REQUIRED")
        void rejectsBlankName() {
            assertThatThrownBy(() -> stageCommandService.createStage(command("   ", null, "MEMBER")))
                    .satisfies(hasCode(StageErrorCode.STAGE_NAME_REQUIRED));
        }

        @Test
        @DisplayName("이름이 100자를 넘으면 STAGE_NAME_TOO_LONG")
        void rejectsTooLongName() {
            String tooLong = "가".repeat(101);
            assertThatThrownBy(() -> stageCommandService.createStage(command(tooLong, null, "MEMBER")))
                    .satisfies(hasCode(StageErrorCode.STAGE_NAME_TOO_LONG));
        }

        @Test
        @DisplayName("이름이 정확히 100자면 통과한다")
        void acceptsExactly100Chars() {
            String exactly100 = "가".repeat(100);
            givenEditableProject();
            when(stageRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(exactly100, 1));

            StageResult result = stageCommandService.createStage(command(exactly100, null, "MEMBER"));

            assertThat(result.name()).isEqualTo(exactly100);
        }
    }

    @Nested
    @DisplayName("프로젝트 접근 검증")
    class ProjectAccessValidation {

        @Test
        @DisplayName("프로젝트가 없으면 PROJECT_NOT_FOUND")
        void rejectsMissingProject() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(false);

            assertThatThrownBy(() -> stageCommandService.createStage(command(VALID_NAME, null, "MEMBER")))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
            verify(stageRepository, never()).save(any());
        }

        @Test
        @DisplayName("참여자가 아니면 PROJECT_EDIT_DENIED")
        void rejectsNonMember() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
            when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stageCommandService.createStage(command(VALID_NAME, null, "MEMBER")))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_EDIT_DENIED));
        }

        @Test
        @DisplayName("VIEWER 권한이면 PROJECT_EDIT_DENIED")
        void rejectsViewer() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
            when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID))
                    .thenReturn(Optional.of(MemberPermission.VIEWER));

            assertThatThrownBy(() -> stageCommandService.createStage(command(VALID_NAME, null, "MEMBER")))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_EDIT_DENIED));
        }

        @Test
        @DisplayName("MASTER 는 참여자가 아니어도 통과한다")
        void allowsGlobalAdminWithoutMembership() {
            when(projectAccessPort.existsProject(PROJECT_ID)).thenReturn(true);
            when(projectAccessPort.findPermission(PROJECT_ID, REQUESTER_ID)).thenReturn(Optional.empty());
            when(stageRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 1));

            stageCommandService.createStage(command(VALID_NAME, null, "MASTER"));

            verify(stageRepository).save(any(Stage.class));
        }

        @Test
        @DisplayName("EDITOR 참여자면 통과한다")
        void allowsEditor() {
            givenEditableProject();
            when(stageRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 1));

            stageCommandService.createStage(command(VALID_NAME, null, "MEMBER"));

            verify(stageRepository).save(any(Stage.class));
        }
    }

    @Nested
    @DisplayName("정렬 순서 처리")
    class SortOrderHandling {

        @Test
        @DisplayName("sortOrder 를 지정하면 그대로 쓰고 max 조회를 하지 않는다")
        void usesGivenSortOrder() {
            givenEditableProject();
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 5));

            StageResult result = stageCommandService.createStage(command(VALID_NAME, 5, "MEMBER"));

            assertThat(result.sortOrder()).isEqualTo(5);
            verify(stageRepository, never()).findMaxSortOrder(anyLong());
        }

        @Test
        @DisplayName("sortOrder 를 생략하고 스테이지가 없으면 1 이 된다")
        void firstStageStartsAtOne() {
            givenEditableProject();
            when(stageRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 1));

            ArgumentCaptor<Stage> captor = ArgumentCaptor.forClass(Stage.class);
            stageCommandService.createStage(command(VALID_NAME, null, "MEMBER"));

            verify(stageRepository).save(captor.capture());
            assertThat(captor.getValue().getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("sortOrder 를 생략하고 기존 최대값이 있으면 max+1 이 된다")
        void nextStageIncrementsMax() {
            givenEditableProject();
            when(stageRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.of(3));
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 4));

            ArgumentCaptor<Stage> captor = ArgumentCaptor.forClass(Stage.class);
            stageCommandService.createStage(command(VALID_NAME, null, "MEMBER"));

            verify(stageRepository).save(captor.capture());
            assertThat(captor.getValue().getSortOrder()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("생성 성공")
    class CreateSuccess {

        @Test
        @DisplayName("정상 요청이면 스테이지를 저장하고 결과를 반환한다")
        void createsStageAndReturnsResult() {
            givenEditableProject();
            when(stageRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 1));

            StageResult result = stageCommandService.createStage(command(VALID_NAME, null, "MEMBER"));

            assertThat(result.stageId()).isEqualTo(STAGE_ID);
            assertThat(result.projectId()).isEqualTo(PROJECT_ID);
            assertThat(result.name()).isEqualTo(VALID_NAME);
            assertThat(result.sortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("저장 전 프로젝트 ID·이름·정렬순서를 그대로 넘긴다")
        void savesWithGivenFields() {
            givenEditableProject();
            when(stageRepository.save(any(Stage.class))).thenReturn(savedStage(VALID_NAME, 9));

            ArgumentCaptor<Stage> captor = ArgumentCaptor.forClass(Stage.class);
            stageCommandService.createStage(command(VALID_NAME, 9, "MEMBER"));

            verify(stageRepository).save(captor.capture());
            Stage toSave = captor.getValue();
            assertThat(toSave.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(toSave.getName()).isEqualTo(VALID_NAME);
            assertThat(toSave.getSortOrder()).isEqualTo(9);
        }
    }
}
