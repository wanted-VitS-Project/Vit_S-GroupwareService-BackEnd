package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.command.ReorderStepsCommand;
import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.application.result.StepOrderResult;
import com.group3.vitamins.project.step.application.result.StepUpdateResult;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 스텝명 필수·길이는 요청 DTO 의 Bean Validation 이 맡으므로 여기서 테스트하지 않는다.
 * 이 파일은 서비스에 남은 규칙(날짜 관계 · 책임자 존재 · 위치 불변 · 재정렬 검증)만 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class StepCommandServiceTest {

    @Mock private StepRepository stepRepository;
    @Mock private StageLookupPort stageLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private StepAccessUseCase stepAccessUseCase;

    @InjectMocks private StepCommandService stepCommandService;

    private static final LocalDate STARTED = LocalDate.of(2026, 8, 1);
    private static final LocalDate ENDED = LocalDate.of(2026, 8, 10);
    private static final Long PROJECT_ID = 3L;

    // ────────────────────────────── 스텝 수정 ──────────────────────────────

    @Test
    @DisplayName("받은 값으로 이름·기간·책임자를 덮어쓴다")
    void 수정() {
        givenStep();
        given(employeeLookupPort.findNameByUserId("E2024007")).willReturn("김동훈");

        StepUpdateResult result = stepCommandService.updateStep(
                command("제안서 작성·검토", STARTED, ENDED, "E2024007"));

        Step saved = captureSaved();
        assertThat(saved.getName()).isEqualTo("제안서 작성·검토");
        assertThat(saved.getOwnerUserId()).isEqualTo("E2024007");
        assertThat(result.owner().name()).isEqualTo("김동훈");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("수정은 위치를 건드리지 않는다 — 스테이지·정렬순서 그대로")
    void 위치_불변() {
        givenStep();

        stepCommandService.updateStep(command("스텝", null, null, null));

        Step saved = captureSaved();
        assertThat(saved.getStageId()).isEqualTo(7L);
        assertThat(saved.getSortOrder()).isEqualTo(5);
        Mockito.verifyNoInteractions(stageLookupPort);
    }

    @Test
    @DisplayName("책임자를 생략하면 해제된다")
    void 책임자_해제() {
        givenStep();

        StepUpdateResult result = stepCommandService.updateStep(command("스텝", null, null, null));

        assertThat(captureSaved().getOwnerUserId()).isNull();
        assertThat(result.owner()).isNull();
    }

    @Test
    @DisplayName("공백만 보낸 책임자도 해제로 저장된다 — 응답과 DB 가 어긋나면 안 된다")
    void 책임자_공백() {
        givenStep();

        StepUpdateResult result = stepCommandService.updateStep(command("스텝", null, null, "   "));

        assertThat(captureSaved().getOwnerUserId()).isNull();
        assertThat(result.owner()).isNull();
        Mockito.verifyNoInteractions(employeeLookupPort);
    }

    @Test
    @DisplayName("스텝 편집 권한이 없으면 조회도 저장도 하지 않는다")
    void 권한_거부() {
        Mockito.doThrow(new ForbiddenException(StepErrorCode.STEP_EDIT_DENIED))
                .when(stepAccessUseCase).requireEditable(10L, "E2024001", "USER");

        assertThatThrownBy(() -> stepCommandService.updateStep(command("스텝", null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(stepRepository);
    }

    @Test
    @DisplayName("없는 책임자는 404 다")
    void 책임자_없음() {
        givenStep();
        given(employeeLookupPort.findNameByUserId("E9999999")).willReturn(null);

        assertThatThrownBy(() -> stepCommandService.updateStep(
                command("스텝", null, null, "E9999999")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 400 이다")
    void 날짜_역전() {
        given(stepAccessUseCase.requireEditable(10L, "E2024001", "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(10L, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(10L)).willReturn(Optional.of(existingStep()));

        assertThatThrownBy(() -> stepCommandService.updateStep(
                command("스텝", ENDED, STARTED, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("수정으로 상태·완료정보는 안 바뀐다")
    void 상태_불변() {
        givenStep();

        stepCommandService.updateStep(command("스텝", null, null, null));

        Step saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(StepStatus.IN_PROGRESS);
        assertThat(saved.getCompletedAt()).isNull();
    }

    // ────────────────────────────── 순서 변경 ──────────────────────────────

    @Test
    @DisplayName("보낸 순서 그대로 스테이지·정렬순서를 확정한다")
    void 순서_변경() {
        givenReorder(stepAt(11L, 7L, 1), stepAt(10L, 7L, 2));
        given(stageLookupPort.existsInProject(9L, PROJECT_ID)).willReturn(true);

        List<StepOrderResult> results = stepCommandService.reorderSteps(reorder(
                item(11L, 9L, 2),
                item(10L, 9L, 1)));

        assertThat(results).extracting(StepOrderResult::stepId).containsExactly(11L, 10L);
        assertThat(results).extracting(StepOrderResult::stageId).containsOnly(9L);
        assertThat(results).extracting(StepOrderResult::sortOrder).containsExactly(2, 1);
    }

    @Test
    @DisplayName("스테이지를 null 로 보내면 미소속으로 빠지고 스테이지 조회를 하지 않는다")
    void 미소속_이동() {
        givenReorder(stepAt(11L, 7L, 1));

        List<StepOrderResult> results = stepCommandService.reorderSteps(reorder(item(11L, null, 1)));

        assertThat(results.get(0).stageId()).isNull();
        Mockito.verifyNoInteractions(stageLookupPort);
    }

    @Test
    @DisplayName("순서 값이 중복되면 400 이다")
    void 순서_중복() {
        assertThatThrownBy(() -> stepCommandService.reorderSteps(reorder(
                item(11L, 7L, 1),
                item(10L, 7L, 1))))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(stepRepository);
    }

    @Test
    @DisplayName("같은 스텝이 두 번 오면 400 이다")
    void 스텝_중복() {
        assertThatThrownBy(() -> stepCommandService.reorderSteps(reorder(
                item(11L, 7L, 1),
                item(11L, 7L, 2))))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(stepRepository);
    }

    @Test
    @DisplayName("목록에 없는 스텝이 섞이면 404 다 — 남의 프로젝트 스텝도 여기서 걸린다")
    void 스텝_없음() {
        given(stepRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stepAt(11L, 7L, 1)));

        assertThatThrownBy(() -> stepCommandService.reorderSteps(reorder(
                item(11L, 7L, 1),
                item(99L, 7L, 2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("남의 프로젝트 스테이지로는 못 옮긴다")
    void 스테이지_없음() {
        given(stepRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stepAt(11L, 7L, 1)));
        given(stageLookupPort.existsInProject(999L, PROJECT_ID)).willReturn(false);

        assertThatThrownBy(() -> stepCommandService.reorderSteps(reorder(item(11L, 999L, 1))))
                .isInstanceOf(NotFoundException.class);
    }

    // ────────────────────────────── 헬퍼 ──────────────────────────────

    private UpdateStepCommand command(String name, LocalDate startedOn,
                                      LocalDate endedOn, String ownerUserId) {
        return new UpdateStepCommand(10L, name, startedOn, endedOn, ownerUserId,
                "E2024001", "USER");
    }

    private ReorderStepsCommand reorder(ReorderStepsCommand.Item... items) {
        return new ReorderStepsCommand(PROJECT_ID, List.of(items), "E2024001", "USER");
    }

    private ReorderStepsCommand.Item item(Long stepId, Long stageId, int sortOrder) {
        return new ReorderStepsCommand.Item(stepId, stageId, sortOrder);
    }

    /** 프로젝트 3 소속, 스테이지 7, 진행 중, 정렬 5 인 기존 스텝. */
    private Step existingStep() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Step.restore(10L, PROJECT_ID, 7L, "기존 스텝", 5, STARTED, ENDED, "E2024001",
                StepStatus.IN_PROGRESS, null, null, createdAt, createdAt, null);
    }

    private Step stepAt(Long stepId, Long stageId, int sortOrder) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Step.restore(stepId, PROJECT_ID, stageId, "스텝 " + stepId, sortOrder,
                null, null, null, StepStatus.NOT_STARTED, null, null, createdAt, createdAt, null);
    }

    private void givenStep() {
        given(stepAccessUseCase.requireEditable(10L, "E2024001", "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(10L, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(10L)).willReturn(Optional.of(existingStep()));
        Mockito.lenient().when(stepRepository.save(any(Step.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenReorder(Step... steps) {
        given(stepRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(steps));
        given(stepRepository.save(any(Step.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private Step captureSaved() {
        ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
        Mockito.verify(stepRepository).save(captor.capture());
        return captor.getValue();
    }
}
