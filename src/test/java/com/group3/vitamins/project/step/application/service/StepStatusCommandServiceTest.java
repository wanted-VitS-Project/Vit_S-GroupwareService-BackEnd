package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private static final int VERSION = 1;

    // ────────────────────────────── 상태 변경 ──────────────────────────────

    @Test
    @DisplayName("IN_PROGRESS 로 바꾸면 상태와 수정일시가 갱신되고 version 이 +1 된다")
    void 상태_변경() {
        givenStep(StepStatus.NOT_STARTED);
        givenStatusUpdated(1);

        StepStatusResult result = stepCommandService.changeStatus(status("IN_PROGRESS", VERSION));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.updatedAt()).isNotNull();
        assertThat(result.version()).isEqualTo(VERSION + 1);
    }

    @Test
    @DisplayName("내가 본 뒤 남이 먼저 저장했으면 409 다")
    void 상태_버전_충돌() {
        givenStep(StepStatus.NOT_STARTED);
        givenStatusUpdated(0);

        assertThatThrownBy(() -> stepCommandService.changeStatus(status("IN_PROGRESS", VERSION)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("DONE 은 이 API 로 못 넣는다 — 400")
    void DONE_거부() {
        givenStep(StepStatus.IN_PROGRESS);

        assertThatThrownBy(() -> stepCommandService.changeStatus(status("DONE", VERSION)))
                .isInstanceOf(ValidationException.class);

        Mockito.verify(stepRepository, Mockito.never())
                .changeStatusIfVersionMatches(anyLong(), any(), any(), any(), any(), anyInt());
    }

    /**
     * ⚠️ 완료 정보를 UPDATE 에 안 실으면 <b>상태만 IN_PROGRESS 로 바뀌고 완료자·완료시각이 DB 에 남는다.</b>
     * 도메인 {@code changeStatus} 는 null 로 만들지만 그 결과를 SQL 에 넘기지 않으면 소용이 없다 —
     * 예외도 안 나고 응답도 정상이라 조회 화면을 봐야만 드러난다.
     */
    @Test
    @DisplayName("DONE 에서 되돌리면 완료자·완료시각도 UPDATE 에 null 로 실려 간다")
    void 완료_해제() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        givenAccess();
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(
                step(StepStatus.DONE, completedAt, "E2024099")));
        givenStatusUpdated(1);

        stepCommandService.changeStatus(status("IN_PROGRESS", VERSION));

        ArgumentCaptor<StepStatus> statusCaptor = ArgumentCaptor.forClass(StepStatus.class);
        ArgumentCaptor<LocalDateTime> completedAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> completedByCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(stepRepository).changeStatusIfVersionMatches(
                eq(STEP_ID), statusCaptor.capture(), completedAtCaptor.capture(),
                completedByCaptor.capture(), any(), eq(VERSION));

        assertThat(statusCaptor.getValue()).isEqualTo(StepStatus.IN_PROGRESS);
        assertThat(completedAtCaptor.getValue()).isNull();
        assertThat(completedByCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("없는 상태 값은 400 이다 — null 이어도 500 이 아니다")
    void 상태_오타() {
        givenStep(StepStatus.NOT_STARTED);

        assertThatThrownBy(() -> stepCommandService.changeStatus(status(null, VERSION)))
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

    /** 완료는 이 규칙이 두 번째 요청을 막으므로 낙관락을 걸지 않는다 (`CONCURRENCY.md`). */
    @Test
    @DisplayName("이미 완료된 스텝은 완료자·완료시각을 덮어쓰지 않는다")
    void 이미_완료() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        givenAccess();
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

    private ChangeStepStatusCommand status(String status, int version) {
        return new ChangeStepStatusCommand(STEP_ID, status, version, false, REQUESTER, "USER");
    }

    private void givenAccess() {
        given(stepAccessUseCase.requireEditable(STEP_ID, REQUESTER, "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(
                        STEP_ID, 3L, MemberPermission.EDITOR));
    }

    private void givenStatusUpdated(int affectedRows) {
        given(stepRepository.changeStatusIfVersionMatches(
                anyLong(), any(), any(), any(), any(), anyInt()))
                .willReturn(affectedRows);
    }

    /** 완료 경로는 여전히 {@code save()} 를 쓴다 — 낙관락은 수정·상태변경·순서에만 건다. */
    private void givenStep(StepStatus status) {
        givenAccess();
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(step(status, null, null)));
        Mockito.lenient().when(stepRepository.save(any(Step.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Step step(StepStatus status, LocalDateTime completedAt, String completedBy) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Step.restore(STEP_ID, 3L, 7L, "제안서 작성", 1, VERSION,
                null, null, REQUESTER,
                status, completedAt, completedBy, createdAt, createdAt, null);
    }
}
