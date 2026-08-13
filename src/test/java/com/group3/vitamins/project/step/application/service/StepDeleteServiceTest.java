package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.step.application.command.DeleteStepCommand;
import com.group3.vitamins.project.step.application.port.IssueCloseCommandPort;
import com.group3.vitamins.project.step.application.port.IssueDeleteCommandPort;
import com.group3.vitamins.project.step.application.port.IssueStatLookupPort;
import com.group3.vitamins.project.step.application.port.StagePermissionDefaultLookupPort;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.application.port.StepBlockCascadePort;
import com.group3.vitamins.project.step.application.result.StepDeleteResult;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/** 스텝 삭제 (STP-013). ⛔ 잠금 없음 — 살릴 블록만 골라 옮기고 나머지는 전부 삭제한다. */
@ExtendWith(MockitoExtension.class)
class StepDeleteServiceTest {

    @Mock private StepRepository stepRepository;
    @Mock private StageLookupPort stageLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private IssueStatLookupPort issueStatLookupPort;
    @Mock private IssueCloseCommandPort issueCloseCommandPort;
    @Mock private IssueDeleteCommandPort issueDeleteCommandPort;
    @Mock private StepBlockCascadePort stepBlockCascadePort;
    @Mock private StagePermissionDefaultLookupPort stagePermissionDefaultLookupPort;
    @Mock private StepPermissionRepository stepPermissionRepository;
    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private StepAccessUseCase stepAccessUseCase;

    @InjectMocks private StepCommandService stepCommandService;

    private static final Long STEP_ID = 10L;
    private static final Long PROJECT_ID = 3L;
    private static final String REQUESTER = "E2024001";

    @Test
    @DisplayName("지정 없이 부르면 블록·이슈를 전부 삭제한다")
    void 전부_삭제() {
        givenStep();
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of(5L, 7L, 9L));
        given(issueStatLookupPort.findAllIssueIds(STEP_ID)).willReturn(List.of(1L, 2L));

        StepDeleteResult result = stepCommandService.deleteStep(command(null, null));

        assertThat(result.movedBlockCount()).isZero();
        assertThat(result.deletedBlockCount()).isEqualTo(3);
        assertThat(result.deletedIssueCount()).isEqualTo(2);
        assertThat(captureSaved().getDeletedAt()).isNotNull();
        Mockito.verify(stepBlockCascadePort).deleteBlocks(List.of(5L, 7L, 9L), REQUESTER);
        Mockito.verify(stepBlockCascadePort, Mockito.never())
                .moveBlocks(anyCollection(), any());
        // 스텝은 복구가 없어 오버라이드가 영구히 남는다 — D-3 예외 (DELETE.md §2-2).
        Mockito.verify(stepPermissionRepository).deleteByStepId(STEP_ID);
    }

    @Test
    @DisplayName("고른 블록만 옮기고 나머지는 삭제한다")
    void 선별_이전() {
        givenStep();
        givenMoveTarget(11L, PROJECT_ID);
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of(5L, 7L, 9L));
        given(issueStatLookupPort.findAllIssueIds(STEP_ID)).willReturn(List.of());

        StepDeleteResult result = stepCommandService.deleteStep(
                command(List.of(5L, 7L), 11L));

        assertThat(result.movedBlockCount()).isEqualTo(2);
        assertThat(result.deletedBlockCount()).isEqualTo(1);
        Mockito.verify(stepBlockCascadePort).moveBlocks(List.of(5L, 7L), 11L);
        Mockito.verify(stepBlockCascadePort).deleteBlocks(List.of(9L), REQUESTER);
    }

    @Test
    @DisplayName("하위 이슈는 선택지 없이 전부 삭제된다")
    void 이슈_전부_삭제() {
        givenStep();
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of());
        given(issueStatLookupPort.findAllIssueIds(STEP_ID)).willReturn(List.of(1L, 2L, 3L));

        stepCommandService.deleteStep(command(null, null));

        Mockito.verify(issueDeleteCommandPort).delete(List.of(1L, 2L, 3L));
        Mockito.verifyNoInteractions(issueCloseCommandPort);
    }

    @Test
    @DisplayName("옮길 블록을 골랐는데 대상 스텝이 없으면 400 이다")
    void 이전_대상_누락() {
        givenStep();
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of(5L));

        assertThatThrownBy(() -> stepCommandService.deleteStep(command(List.of(5L), null)))
                .isInstanceOf(ValidationException.class);

        Mockito.verify(stepRepository, Mockito.never()).save(any(Step.class));
    }

    @Test
    @DisplayName("자기 자신으로는 못 옮긴다 — 400")
    void 이전_대상_자기자신() {
        givenStep();
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of(5L));

        assertThatThrownBy(() -> stepCommandService.deleteStep(command(List.of(5L), STEP_ID)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("다른 프로젝트 스텝으로는 못 옮긴다 — 400")
    void 이전_대상_남의_프로젝트() {
        givenStep();
        givenMoveTarget(11L, 99L);
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of(5L));

        assertThatThrownBy(() -> stepCommandService.deleteStep(command(List.of(5L), 11L)))
                .isInstanceOf(ValidationException.class);

        Mockito.verify(stepBlockCascadePort, Mockito.never()).moveBlocks(anyCollection(), any());
    }

    @Test
    @DisplayName("이 스텝 소속이 아닌 블록을 고르면 404 다")
    void 남의_블록() {
        givenStep();
        givenMoveTarget(11L, PROJECT_ID);
        given(stepBlockCascadePort.findBlockIds(STEP_ID)).willReturn(List.of(5L));

        assertThatThrownBy(() -> stepCommandService.deleteStep(command(List.of(5L, 99L), 11L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("없는 스텝은 404 다 — 권한 판정까지 가지 않는다")
    void 스텝_없음() {
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stepCommandService.deleteStep(command(null, null)))
                .isInstanceOf(NotFoundException.class);

        Mockito.verifyNoInteractions(projectAccessUseCase, stepBlockCascadePort);
    }

    private DeleteStepCommand command(List<Long> moveBlockIds, Long moveToStepId) {
        return new DeleteStepCommand(STEP_ID, moveBlockIds, moveToStepId, REQUESTER, "USER");
    }

    private void givenStep() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(
                Step.restore(STEP_ID, PROJECT_ID, 7L, "제안서 작성", 1, 1, null, null, null,
                        StepStatus.IN_PROGRESS, null, null, createdAt, createdAt, null)));
        Mockito.lenient().when(stepRepository.save(any(Step.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** 블록 이전 대상 스텝. cascade 이동은 권한 판정을 건너뛰므로 서비스가 직접 프로젝트를 확인한다. */
    private void givenMoveTarget(Long stepId, Long projectId) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        given(stepRepository.findById(stepId)).willReturn(Optional.of(
                Step.restore(stepId, projectId, 7L, "옮길 곳", 2, 1, null, null, null,
                        StepStatus.NOT_STARTED, null, null, createdAt, createdAt, null)));
    }

    private Step captureSaved() {
        ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
        Mockito.verify(stepRepository).save(captor.capture());
        return captor.getValue();
    }
}
