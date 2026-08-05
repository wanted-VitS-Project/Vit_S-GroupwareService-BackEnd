package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("StepCommandService 스텝 생성")
class StepCommandServiceTest {

    private static final Long PROJECT_ID = 12L;
    private static final Long STAGE_ID = 7L;
    private static final Long STEP_ID = 10L;
    private static final String REQUESTER_ID = "E2024001";
    private static final String ROLE = "MEMBER";
    private static final String VALID_NAME = "제안서 작성";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 10, 0);

    private StepRepository stepRepository;
    private StageLookupPort stageLookupPort;
    private EmployeeLookupPort employeeLookupPort;
    private ProjectAccessUseCase projectAccessUseCase;
    private StepCommandService stepCommandService;

    @BeforeEach
    void setUp() {
        stepRepository = Mockito.mock(StepRepository.class);
        stageLookupPort = Mockito.mock(StageLookupPort.class);
        employeeLookupPort = Mockito.mock(EmployeeLookupPort.class);
        projectAccessUseCase = Mockito.mock(ProjectAccessUseCase.class);
        stepCommandService = new StepCommandService(
                stepRepository, stageLookupPort, employeeLookupPort, projectAccessUseCase);
    }

    private CreateStepCommand command(String name, Long stageId, LocalDate startedOn, LocalDate endedOn,
                                       String ownerUserId) {
        return new CreateStepCommand(PROJECT_ID, stageId, name, startedOn, endedOn, ownerUserId,
                REQUESTER_ID, ROLE);
    }

    private CreateStepCommand minimalCommand(String name) {
        return command(name, null, null, null, null);
    }

    private Step savedStep(Long stageId, String name, int sortOrder, String ownerUserId) {
        return Step.restore(STEP_ID, PROJECT_ID, stageId, name, sortOrder, null, null, ownerUserId,
                StepStatus.NOT_STARTED, null, null, NOW, NOW, null);
    }

    private Consumer<Throwable> hasCode(ErrorCode expected) {
        return thrown -> {
            assertThat(thrown).isInstanceOf(DomainException.class);
            assertThat(((DomainException) thrown).getErrorCode()).isEqualTo(expected);
        };
    }

    @Nested
    @DisplayName("스텝명 검증")
    class NameValidation {

        @Test
        @DisplayName("이름이 null 이면 STEP_NAME_REQUIRED")
        void rejectsNullName() {
            assertThatThrownBy(() -> stepCommandService.createStep(minimalCommand(null)))
                    .satisfies(hasCode(StepErrorCode.STEP_NAME_REQUIRED));
            verify(projectAccessUseCase, never()).requireEditable(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("이름이 공백이면 STEP_NAME_REQUIRED")
        void rejectsBlankName() {
            assertThatThrownBy(() -> stepCommandService.createStep(minimalCommand("   ")))
                    .satisfies(hasCode(StepErrorCode.STEP_NAME_REQUIRED));
        }

        @Test
        @DisplayName("이름이 200자를 넘으면 STEP_NAME_TOO_LONG")
        void rejectsTooLongName() {
            String tooLong = "가".repeat(201);
            assertThatThrownBy(() -> stepCommandService.createStep(minimalCommand(tooLong)))
                    .satisfies(hasCode(StepErrorCode.STEP_NAME_TOO_LONG));
        }

        @Test
        @DisplayName("이름이 정확히 200자면 통과한다")
        void acceptsExactly200Chars() {
            String exactly200 = "가".repeat(200);
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, exactly200, 1, null));

            StepResult result = stepCommandService.createStep(minimalCommand(exactly200));

            assertThat(result.name()).isEqualTo(exactly200);
        }
    }

    @Nested
    @DisplayName("기간 검증")
    class DateRangeValidation {

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 STEP_DATE_RANGE_INVALID")
        void rejectsStartAfterEnd() {
            CreateStepCommand cmd = command(VALID_NAME, null,
                    LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1), null);

            assertThatThrownBy(() -> stepCommandService.createStep(cmd))
                    .satisfies(hasCode(StepErrorCode.STEP_DATE_RANGE_INVALID));
        }

        @Test
        @DisplayName("시작일과 종료일이 같으면 통과한다")
        void allowsSameStartAndEnd() {
            LocalDate day = LocalDate.of(2026, 8, 1);
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, null));

            StepResult result = stepCommandService.createStep(command(VALID_NAME, null, day, day, null));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("시작일만 있어도 통과한다")
        void allowsOnlyStartedOn() {
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, null));

            stepCommandService.createStep(
                    command(VALID_NAME, null, LocalDate.of(2026, 8, 1), null, null));
        }

        @Test
        @DisplayName("둘 다 없어도 통과한다")
        void allowsNoDates() {
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, null));

            stepCommandService.createStep(minimalCommand(VALID_NAME));
        }
    }

    @Nested
    @DisplayName("프로젝트 접근 검증 — ProjectAccessUseCase 위임")
    class ProjectAccessValidation {

        @Test
        @DisplayName("프로젝트가 없으면 PROJECT_NOT_FOUND 가 그대로 전파된다")
        void propagatesProjectNotFound() {
            doThrow(new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND))
                    .when(projectAccessUseCase).requireEditable(PROJECT_ID, REQUESTER_ID, ROLE);

            assertThatThrownBy(() -> stepCommandService.createStep(minimalCommand(VALID_NAME)))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
            verify(stepRepository, never()).save(any());
        }

        @Test
        @DisplayName("편집 권한이 없으면 PROJECT_EDIT_DENIED 가 그대로 전파된다")
        void propagatesEditDenied() {
            doThrow(new ForbiddenException(ProjectErrorCode.PROJECT_EDIT_DENIED))
                    .when(projectAccessUseCase).requireEditable(PROJECT_ID, REQUESTER_ID, ROLE);

            assertThatThrownBy(() -> stepCommandService.createStep(minimalCommand(VALID_NAME)))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_EDIT_DENIED));
        }

        @Test
        @DisplayName("이름·기간 검증을 통과한 뒤에 접근을 확인한다")
        void checksAccessAfterFieldValidation() {
            assertThatThrownBy(() -> stepCommandService.createStep(minimalCommand(null)))
                    .satisfies(hasCode(StepErrorCode.STEP_NAME_REQUIRED));
            verify(projectAccessUseCase, never()).requireEditable(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("스테이지 소속 검증")
    class StageValidation {

        @Test
        @DisplayName("stageId 가 null 이면 스테이지 검증을 하지 않는다")
        void skipsCheckWhenStageIdNull() {
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, null));

            stepCommandService.createStep(minimalCommand(VALID_NAME));

            verify(stageLookupPort, never()).existsInProject(any(), any());
        }

        @Test
        @DisplayName("stageId 가 프로젝트 소속이 아니면 STAGE_NOT_FOUND")
        void rejectsStageNotInProject() {
            when(stageLookupPort.existsInProject(STAGE_ID, PROJECT_ID)).thenReturn(false);

            CreateStepCommand cmd = command(VALID_NAME, STAGE_ID, null, null, null);

            assertThatThrownBy(() -> stepCommandService.createStep(cmd))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(hasCode(StageErrorCode.STAGE_NOT_FOUND));
            verify(stepRepository, never()).save(any());
        }

        @Test
        @DisplayName("stageId 가 프로젝트 소속이면 통과한다")
        void allowsStageInProject() {
            when(stageLookupPort.existsInProject(STAGE_ID, PROJECT_ID)).thenReturn(true);
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(STAGE_ID, VALID_NAME, 1, null));

            StepResult result = stepCommandService.createStep(
                    command(VALID_NAME, STAGE_ID, null, null, null));

            assertThat(result.stageId()).isEqualTo(STAGE_ID);
        }
    }

    @Nested
    @DisplayName("책임자 확인")
    class OwnerResolution {

        @Test
        @DisplayName("ownerUserId 가 없으면 owner 는 null 이고 사번 조회를 하지 않는다")
        void skipsLookupWhenOwnerUserIdMissing() {
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, null));

            StepResult result = stepCommandService.createStep(minimalCommand(VALID_NAME));

            assertThat(result.owner()).isNull();
            verify(employeeLookupPort, never()).findNameByUserId(any());
        }

        @Test
        @DisplayName("ownerUserId 가 존재하지 않으면 USER_NOT_FOUND")
        void rejectsUnknownOwner() {
            when(employeeLookupPort.findNameByUserId("E9999999")).thenReturn(null);

            CreateStepCommand cmd = command(VALID_NAME, null, null, null, "E9999999");

            assertThatThrownBy(() -> stepCommandService.createStep(cmd))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(hasCode(ProjectErrorCode.USER_NOT_FOUND));
            verify(stepRepository, never()).save(any());
        }

        @Test
        @DisplayName("ownerUserId 가 존재하면 책임자 정보를 채운다")
        void fillsOwnerWhenFound() {
            when(employeeLookupPort.findNameByUserId("E2024002")).thenReturn("김용준");
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, "E2024002"));

            StepResult result = stepCommandService.createStep(
                    command(VALID_NAME, null, null, null, "E2024002"));

            assertThat(result.owner()).isEqualTo(new StepResult.Owner("E2024002", "김용준"));
        }
    }

    @Nested
    @DisplayName("정렬 순서 처리")
    class SortOrderHandling {

        @Test
        @DisplayName("스텝이 없으면 1 부터 시작한다 (프로젝트 전체 기준)")
        void firstStepStartsAtOne() {
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 1, null));

            ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
            stepCommandService.createStep(minimalCommand(VALID_NAME));

            verify(stepRepository).save(captor.capture());
            assertThat(captor.getValue().getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("기존 최대값이 있으면 max+1 이 된다")
        void nextStepIncrementsMax() {
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.of(5));
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(null, VALID_NAME, 6, null));

            ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
            stepCommandService.createStep(minimalCommand(VALID_NAME));

            verify(stepRepository).save(captor.capture());
            assertThat(captor.getValue().getSortOrder()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("생성 성공")
    class CreateSuccess {

        @Test
        @DisplayName("정상 요청이면 스텝을 저장하고 결과를 반환한다")
        void createsStepAndReturnsResult() {
            LocalDate started = LocalDate.of(2026, 8, 1);
            LocalDate ended = LocalDate.of(2026, 8, 10);
            when(stageLookupPort.existsInProject(STAGE_ID, PROJECT_ID)).thenReturn(true);
            when(employeeLookupPort.findNameByUserId("E2024002")).thenReturn("김용준");
            when(stepRepository.findMaxSortOrder(PROJECT_ID)).thenReturn(Optional.empty());
            when(stepRepository.save(any(Step.class)))
                    .thenReturn(savedStep(STAGE_ID, VALID_NAME, 1, "E2024002"));

            StepResult result = stepCommandService.createStep(
                    command(VALID_NAME, STAGE_ID, started, ended, "E2024002"));

            assertThat(result.stepId()).isEqualTo(STEP_ID);
            assertThat(result.projectId()).isEqualTo(PROJECT_ID);
            assertThat(result.stageId()).isEqualTo(STAGE_ID);
            assertThat(result.name()).isEqualTo(VALID_NAME);
            assertThat(result.status()).isEqualTo("NOT_STARTED");
            assertThat(result.sortOrder()).isEqualTo(1);
            assertThat(result.owner()).isEqualTo(new StepResult.Owner("E2024002", "김용준"));
        }

        @Test
        @DisplayName("저장 전 프로젝트 ID·스테이지 ID·이름·기간·책임자를 그대로 넘긴다")
        void savesWithGivenFields() {
            LocalDate started = LocalDate.of(2026, 8, 1);
            LocalDate ended = LocalDate.of(2026, 8, 10);
            when(stageLookupPort.existsInProject(STAGE_ID, PROJECT_ID)).thenReturn(true);
            when(stepRepository.save(any(Step.class))).thenReturn(savedStep(STAGE_ID, VALID_NAME, 1, null));

            ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
            stepCommandService.createStep(command(VALID_NAME, STAGE_ID, started, ended, null));

            verify(stepRepository).save(captor.capture());
            Step toSave = captor.getValue();
            assertThat(toSave.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(toSave.getStageId()).isEqualTo(STAGE_ID);
            assertThat(toSave.getName()).isEqualTo(VALID_NAME);
            assertThat(toSave.getStartedOn()).isEqualTo(started);
            assertThat(toSave.getEndedOn()).isEqualTo(ended);
            assertThat(toSave.getStatus()).isEqualTo(StepStatus.NOT_STARTED);
        }
    }
}
