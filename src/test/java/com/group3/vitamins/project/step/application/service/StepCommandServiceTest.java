package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.command.ReorderStepsCommand;
import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import com.group3.vitamins.project.step.application.port.StagePermissionDefaultLookupPort;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 스텝명 필수·길이는 요청 DTO 의 Bean Validation 이 맡으므로 여기서 테스트하지 않는다.
 * 이 파일은 서비스에 남은 규칙(날짜 관계 · 책임자 존재 · 위치 불변 · 재정렬 검증 · 낙관락)만 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class StepCommandServiceTest {

    @Mock private StepRepository stepRepository;
    @Mock private StageLookupPort stageLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private StagePermissionDefaultLookupPort stagePermissionDefaultLookupPort;
    @Mock private StepPermissionRepository stepPermissionRepository;
    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private StepAccessUseCase stepAccessUseCase;

    @InjectMocks private StepCommandService stepCommandService;

    private static final LocalDate STARTED = LocalDate.of(2026, 8, 1);
    private static final LocalDate ENDED = LocalDate.of(2026, 8, 10);
    private static final Long PROJECT_ID = 3L;
    private static final Long STEP_ID = 10L;
    private static final int VERSION = 1;

    // ────────────────────────────── 스텝 수정 ──────────────────────────────

    @Test
    @DisplayName("받은 값으로 이름·기간·책임자를 덮어쓴다 — 응답 version 은 +1 된다")
    void 수정() {
        givenStep();
        given(employeeLookupPort.findNameByUserId("E2024007")).willReturn("김동훈");

        StepUpdateResult result = stepCommandService.updateStep(
                command("제안서 작성·검토", STARTED, ENDED, "E2024007"));

        Updated updated = captureUpdated();
        assertThat(updated.name()).isEqualTo("제안서 작성·검토");
        assertThat(updated.ownerUserId()).isEqualTo("E2024007");
        assertThat(updated.expectedVersion()).isEqualTo(VERSION);
        assertThat(result.owner().name()).isEqualTo("김동훈");
        assertThat(result.updatedAt()).isNotNull();
        assertThat(result.version()).isEqualTo(VERSION + 1);
    }

    @Test
    @DisplayName("내가 본 뒤 남이 먼저 저장했으면 409 다 — 조건부 UPDATE 가 0행을 돌려준다")
    void 수정_버전_충돌() {
        givenStepFound();
        given(stepRepository.updateIfVersionMatches(
                eq(STEP_ID), anyString(), any(), any(), any(), any(), eq(VERSION)))
                .willReturn(0);

        assertThatThrownBy(() -> stepCommandService.updateStep(
                command("스텝", null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("덮어쓰기는 DB 현재 버전을 조건으로 써서 통과한다 — 요청이 든 낡은 버전은 무시한다")
    void 수정_덮어쓰기() {
        // DB 는 이미 v5 인데 요청은 v1 을 들고 왔다 (= 그냥 저장하면 409 나는 상황)
        given(stepAccessUseCase.requireEditable(STEP_ID, "E2024001", "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(STEP_ID, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(existingStep(5)));
        given(stepRepository.updateIfVersionMatches(
                eq(STEP_ID), anyString(), any(), any(), any(), any(), eq(5)))
                .willReturn(1);

        StepUpdateResult result = stepCommandService.updateStep(
                new UpdateStepCommand(STEP_ID, "스텝", null, null, null,
                        VERSION, true, "E2024001", "USER"));

        assertThat(result.version()).isEqualTo(6);
    }

    @Test
    @DisplayName("수정은 위치를 건드리지 않는다 — 스테이지·정렬순서 그대로")
    void 위치_불변() {
        givenStep();

        StepUpdateResult result = stepCommandService.updateStep(command("스텝", null, null, null));

        assertThat(result.stageId()).isEqualTo(7L);
        Mockito.verify(stepRepository, Mockito.never())
                .moveIfVersionMatches(anyLong(), any(), anyInt(), any(), anyInt());
        Mockito.verifyNoInteractions(stageLookupPort);
    }

    @Test
    @DisplayName("책임자를 생략하면 해제된다")
    void 책임자_해제() {
        givenStep();

        StepUpdateResult result = stepCommandService.updateStep(command("스텝", null, null, null));

        assertThat(captureUpdated().ownerUserId()).isNull();
        assertThat(result.owner()).isNull();
    }

    @Test
    @DisplayName("공백만 보낸 책임자도 해제로 저장된다 — 응답과 DB 가 어긋나면 안 된다")
    void 책임자_공백() {
        givenStep();

        StepUpdateResult result = stepCommandService.updateStep(command("스텝", null, null, "   "));

        assertThat(captureUpdated().ownerUserId()).isNull();
        assertThat(result.owner()).isNull();
        Mockito.verifyNoInteractions(employeeLookupPort);
    }

    @Test
    @DisplayName("스텝 편집 권한이 없으면 조회도 저장도 하지 않는다")
    void 권한_거부() {
        Mockito.doThrow(new ForbiddenException(StepErrorCode.STEP_EDIT_DENIED))
                .when(stepAccessUseCase).requireEditable(STEP_ID, "E2024001", "USER");

        assertThatThrownBy(() -> stepCommandService.updateStep(command("스텝", null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(stepRepository);
    }

    @Test
    @DisplayName("없는 책임자는 404 다")
    void 책임자_없음() {
        givenStepFound();
        given(employeeLookupPort.findNameByUserId("E9999999")).willReturn(null);

        assertThatThrownBy(() -> stepCommandService.updateStep(
                command("스텝", null, null, "E9999999")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 400 이다")
    void 날짜_역전() {
        givenStepFound();

        assertThatThrownBy(() -> stepCommandService.updateStep(
                command("스텝", ENDED, STARTED, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("수정은 상태·완료정보를 건드리지 않는다 — 상태 전용 UPDATE 를 부르지 않는다")
    void 상태_불변() {
        givenStep();

        stepCommandService.updateStep(command("스텝", null, null, null));

        Mockito.verify(stepRepository, Mockito.never())
                .changeStatusIfVersionMatches(anyLong(), any(), any(), any(), any(), anyInt());
    }

    // ────────────────────────────── 순서 변경 ──────────────────────────────

    @Test
    @DisplayName("보낸 순서 그대로 스테이지·정렬순서를 확정한다 — 항목마다 version 이 +1 된다")
    void 순서_변경() {
        givenReorder(stepAt(11L, 7L, 1), stepAt(10L, 7L, 2));
        given(stageLookupPort.existsInProject(9L, PROJECT_ID)).willReturn(true);

        List<StepOrderResult> results = stepCommandService.reorderSteps(reorder(
                item(11L, 9L, 2),
                item(10L, 9L, 1)));

        assertThat(results).extracting(StepOrderResult::stepId).containsExactly(11L, 10L);
        assertThat(results).extracting(StepOrderResult::stageId).containsOnly(9L);
        assertThat(results).extracting(StepOrderResult::sortOrder).containsExactly(2, 1);
        assertThat(results).extracting(StepOrderResult::version)
                .containsExactly(VERSION + 1, VERSION + 1);
    }

    /**
     * ⚠️ 여기서 예외가 안 새어 나오면 <b>보드가 반쯤 바뀐 채 커밋된다.</b>
     * 서비스가 충돌을 삼키거나 try-catch 로 감싸면 이 테스트가 깨진다 (`CONCURRENCY.md` §4-3).
     */
    @Test
    @DisplayName("항목 하나만 충돌해도 요청 전체가 409 다 — 앞 항목만 저장되면 안 된다")
    void 순서_부분_충돌() {
        given(stepRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stepAt(11L, 7L, 1), stepAt(10L, 7L, 2)));
        given(stepRepository.moveIfVersionMatches(eq(11L), any(), eq(2), any(), eq(VERSION)))
                .willReturn(1);
        given(stepRepository.moveIfVersionMatches(eq(10L), any(), eq(1), any(), eq(VERSION)))
                .willReturn(0);

        assertThatThrownBy(() -> stepCommandService.reorderSteps(reorder(
                item(11L, null, 2),
                item(10L, null, 1))))
                .isInstanceOf(ConflictException.class);
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
    @DisplayName("요청에 없는 기존 스텝과 순서가 겹치면 400 이다 — 부분 전송을 막는다")
    void 순서_기존행_충돌() {
        given(stepRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stepAt(11L, 7L, 1)));
        given(stepRepository.search(PROJECT_ID, null, null))
                .willReturn(List.of(stepAt(11L, 7L, 1), stepAt(10L, 7L, 2)));

        assertThatThrownBy(() -> stepCommandService.reorderSteps(reorder(item(11L, 7L, 2))))
                .isInstanceOf(ValidationException.class);

        Mockito.verify(stepRepository, Mockito.never())
                .moveIfVersionMatches(anyLong(), any(), anyInt(), any(), anyInt());
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

    /** 조건부 UPDATE 에 실제로 실려 간 값. save() 를 안 쓰므로 엔티티를 캡처할 수 없다. */
    private record Updated(String name, LocalDate startedOn, LocalDate endedOn,
                           String ownerUserId, int expectedVersion) {
    }

    private Updated captureUpdated() {
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> startedOn = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endedOn = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> ownerUserId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> expectedVersion = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(stepRepository).updateIfVersionMatches(
                eq(STEP_ID), name.capture(), startedOn.capture(), endedOn.capture(),
                ownerUserId.capture(), any(), expectedVersion.capture());

        return new Updated(name.getValue(), startedOn.getValue(), endedOn.getValue(),
                ownerUserId.getValue(), expectedVersion.getValue());
    }

    private UpdateStepCommand command(String name, LocalDate startedOn,
                                      LocalDate endedOn, String ownerUserId) {
        return new UpdateStepCommand(STEP_ID, name, startedOn, endedOn, ownerUserId,
                VERSION, false, "E2024001", "USER");
    }

    private ReorderStepsCommand reorder(ReorderStepsCommand.Item... items) {
        return new ReorderStepsCommand(PROJECT_ID, List.of(items), "E2024001", "USER");
    }

    private ReorderStepsCommand.Item item(Long stepId, Long stageId, int sortOrder) {
        return new ReorderStepsCommand.Item(stepId, stageId, sortOrder, VERSION);
    }

    /** 프로젝트 3 소속, 스테이지 7, 진행 중, 정렬 5 인 기존 스텝. */
    private Step existingStep() {
        return existingStep(VERSION);
    }

    private Step existingStep(int version) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Step.restore(STEP_ID, PROJECT_ID, 7L, "기존 스텝", 5, version,
                STARTED, ENDED, "E2024001",
                StepStatus.IN_PROGRESS, null, null, createdAt, createdAt, null);
    }

    private Step stepAt(Long stepId, Long stageId, int sortOrder) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Step.restore(stepId, PROJECT_ID, stageId, "스텝 " + stepId, sortOrder, VERSION,
                null, null, null, StepStatus.NOT_STARTED, null, null, createdAt, createdAt, null);
    }

    /** 권한·조회만 세팅한다. 조건부 UPDATE 까지 못 가는 케이스(검증 실패)용. */
    private void givenStepFound() {
        given(stepAccessUseCase.requireEditable(STEP_ID, "E2024001", "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(STEP_ID, 3L, MemberPermission.EDITOR));
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(existingStep()));
    }

    private void givenStep() {
        givenStepFound();
        Mockito.lenient().when(stepRepository.updateIfVersionMatches(
                        eq(STEP_ID), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(1);
    }

    private void givenReorder(Step... steps) {
        given(stepRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(steps));
        given(stepRepository.moveIfVersionMatches(anyLong(), any(), anyInt(), any(), anyInt()))
                .willReturn(1);
    }
}
