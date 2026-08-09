package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.command.ChangeStepStatusCommand;
import com.group3.vitamins.project.step.application.command.CompleteStepCommand;
import com.group3.vitamins.project.step.application.port.IssueCloseCommandPort;
import com.group3.vitamins.project.step.application.port.IssueStatLookupPort;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.application.result.StepCompleteResult;
import com.group3.vitamins.project.step.application.result.StepStatusResult;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
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
import static org.mockito.BDDMockito.given;

/** 상태 변경(STP-004)과 완료 처리(STP-005·006)만 다룬다. 수정·재정렬은 StepCommandServiceTest 에 있다. */
@ExtendWith(MockitoExtension.class)
class StepStatusCommandServiceTest {

    @Mock private StepRepository stepRepository;
    @Mock private StageLookupPort stageLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private IssueStatLookupPort issueStatLookupPort;
    @Mock private IssueCloseCommandPort issueCloseCommandPort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private StepAccessUseCase stepAccessUseCase;

    @InjectMocks private StepCommandService stepCommandService;

    private static final Long STEP_ID = 10L;
    private static final String REQUESTER = "E2024001";

    // ────────────────────────────── 상태 변경 ──────────────────────────────

    @Test
    @DisplayName("IN_PROGRESS 로 바꾸면 상태와 수정일시가 갱신된다")
    void 상태_변경() {
        givenStep(StepStatus.NOT_STARTED);

        StepStatusResult result = stepCommandService.changeStatus(
                new ChangeStepStatusCommand(STEP_ID, "IN_PROGRESS", REQUESTER, "USER"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("DONE 은 이 API 로 못 넣는다 — 400")
    void DONE_거부() {
        givenStep(StepStatus.IN_PROGRESS);

        assertThatThrownBy(() -> stepCommandService.changeStatus(
                new ChangeStepStatusCommand(STEP_ID, "DONE", REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);

        Mockito.verify(stepRepository, Mockito.never()).save(any(Step.class));
    }

    @Test
    @DisplayName("DONE 에서 되돌리면 완료자·완료시각도 지워진다 — 진행 중인데 완료 기록이 남으면 안 된다")
    void 완료_해제() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        given(stepAccessUseCase.requireEditable(STEP_ID, REQUESTER, "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(
                        STEP_ID, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(
                step(StepStatus.DONE, completedAt, "E2024099")));
        given(stepRepository.save(any(Step.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        stepCommandService.changeStatus(
                new ChangeStepStatusCommand(STEP_ID, "IN_PROGRESS", REQUESTER, "USER"));

        ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
        Mockito.verify(stepRepository).save(captor.capture());
        Step saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(StepStatus.IN_PROGRESS);
        assertThat(saved.getCompletedBy()).isNull();
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("없는 상태 값은 400 이다 — null 이어도 500 이 아니다")
    void 상태_오타() {
        givenStep(StepStatus.NOT_STARTED);

        assertThatThrownBy(() -> stepCommandService.changeStatus(
                new ChangeStepStatusCommand(STEP_ID, null, REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);
    }

    // ────────────────────────────── 완료 처리 ──────────────────────────────

    @Test
    @DisplayName("KEEP 이면 이슈를 건드리지 않고 스텝만 완료한다")
    void KEEP_완료() {
        givenStep(StepStatus.IN_PROGRESS);
        given(issueStatLookupPort.findOpenIssueIds(STEP_ID)).willReturn(List.of(1L, 2L, 3L));
        given(employeeLookupPort.findNameByUserId(REQUESTER)).willReturn("김동훈");

        StepCompleteResult result = stepCommandService.completeStep(
                new CompleteStepCommand(STEP_ID, "KEEP", REQUESTER, "USER"));

        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.openIssueCount()).isEqualTo(3);
        assertThat(result.closedIssueCount()).isZero();
        assertThat(result.completedBy().name()).isEqualTo("김동훈");
        assertThat(result.completedAt()).isNotNull();
        Mockito.verifyNoInteractions(issueCloseCommandPort);
    }

    @Test
    @DisplayName("CLOSE 면 미완료 이슈를 전부 닫는다")
    void CLOSE_완료() {
        givenStep(StepStatus.IN_PROGRESS);
        given(issueStatLookupPort.findOpenIssueIds(STEP_ID)).willReturn(List.of(1L, 2L, 3L));

        StepCompleteResult result = stepCommandService.completeStep(
                new CompleteStepCommand(STEP_ID, "CLOSE", REQUESTER, "USER"));

        assertThat(result.closedIssueCount()).isEqualTo(3);
        Mockito.verify(issueCloseCommandPort).close(1L, REQUESTER, "USER");
        Mockito.verify(issueCloseCommandPort).close(2L, REQUESTER, "USER");
        Mockito.verify(issueCloseCommandPort).close(3L, REQUESTER, "USER");
    }

    @Test
    @DisplayName("처리 방식 오타는 400 이다")
    void 처리방식_오타() {
        givenStep(StepStatus.IN_PROGRESS);

        assertThatThrownBy(() -> stepCommandService.completeStep(
                new CompleteStepCommand(STEP_ID, "DELETE", REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(issueCloseCommandPort);
    }

    @Test
    @DisplayName("이미 완료된 스텝은 완료자·완료시각을 덮어쓰지 않는다")
    void 이미_완료() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        given(stepAccessUseCase.requireEditable(STEP_ID, REQUESTER, "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(
                        STEP_ID, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(
                step(StepStatus.DONE, completedAt, "E2024099")));
        given(issueStatLookupPort.findOpenIssueIds(STEP_ID)).willReturn(List.of());
        given(employeeLookupPort.findNameByUserId("E2024099")).willReturn("김용준");

        StepCompleteResult result = stepCommandService.completeStep(
                new CompleteStepCommand(STEP_ID, "KEEP", REQUESTER, "USER"));

        assertThat(result.completedBy().userId()).isEqualTo("E2024099");
        assertThat(result.completedAt()).isEqualTo(completedAt);
        Mockito.verify(stepRepository, Mockito.never()).save(any(Step.class));
    }

    // ────────────────────────────── 헬퍼 ──────────────────────────────

    private void givenStep(StepStatus status) {
        given(stepAccessUseCase.requireEditable(STEP_ID, REQUESTER, "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(
                        STEP_ID, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(step(status, null, null)));
        Mockito.lenient().when(stepRepository.save(any(Step.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Step step(StepStatus status, LocalDateTime completedAt, String completedBy) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Step.restore(STEP_ID, 3L, 7L, "제안서 작성", 1, null, null, REQUESTER,
                status, completedAt, completedBy, createdAt, createdAt, null);
    }
}
