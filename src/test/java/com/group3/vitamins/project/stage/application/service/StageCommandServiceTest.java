package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.stage.application.command.DeleteStageCommand;
import com.group3.vitamins.project.stage.application.command.ReorderStagesCommand;
import com.group3.vitamins.project.stage.application.command.UpdateStageCommand;
import com.group3.vitamins.project.stage.application.port.StepRelocationPort;
import com.group3.vitamins.project.stage.application.result.StageDeleteResult;
import com.group3.vitamins.project.stage.application.result.StageOrderResult;
import com.group3.vitamins.project.stage.application.result.StageResult;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/** 스테이지 수정·순서·삭제 (STG-001~003). 이름 필수·길이는 요청 DTO 가 맡는다. */
@ExtendWith(MockitoExtension.class)
class StageCommandServiceTest {

    @Mock private StageRepository stageRepository;
    @Mock private StagePermissionDefaultRepository stagePermissionDefaultRepository;
    @Mock private StepRelocationPort stepRelocationPort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;

    @InjectMocks private StageCommandService stageCommandService;

    private static final Long STAGE_ID = 7L;
    private static final Long PROJECT_ID = 3L;
    private static final String REQUESTER = "E2024001";

    // ────────────────────────────── 수정 ──────────────────────────────

    @Test
    @DisplayName("이름만 바뀌고 정렬 순서는 그대로다")
    void 수정() {
        givenStage();

        StageResult result = stageCommandService.updateStage(
                new UpdateStageCommand(STAGE_ID, "제안·계약", REQUESTER, "USER"));

        assertThat(result.name()).isEqualTo("제안·계약");
        assertThat(result.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 스테이지는 404 다 — 권한 판정까지 가지 않는다")
    void 수정_스테이지_없음() {
        given(stageRepository.findById(STAGE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stageCommandService.updateStage(
                new UpdateStageCommand(STAGE_ID, "제안", REQUESTER, "USER")))
                .isInstanceOf(NotFoundException.class);

        Mockito.verifyNoInteractions(projectAccessUseCase);
    }

    // ────────────────────────────── 순서 ──────────────────────────────

    @Test
    @DisplayName("보낸 순서 그대로 정렬 순서를 확정한다")
    void 순서_변경() {
        given(stageRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stage(7L, 1), stage(8L, 2)));
        given(stageRepository.save(any(Stage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<StageOrderResult> results = stageCommandService.reorderStages(reorder(
                new ReorderStagesCommand.Item(8L, 1),
                new ReorderStagesCommand.Item(7L, 2)));

        assertThat(results).extracting(StageOrderResult::stageId).containsExactly(8L, 7L);
        assertThat(results).extracting(StageOrderResult::sortOrder).containsExactly(1, 2);
    }

    @Test
    @DisplayName("순서 값이 중복되면 400 이다")
    void 순서_중복() {
        assertThatThrownBy(() -> stageCommandService.reorderStages(reorder(
                new ReorderStagesCommand.Item(7L, 1),
                new ReorderStagesCommand.Item(8L, 1))))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(stageRepository);
    }

    @Test
    @DisplayName("요청에 없는 기존 스테이지와 순서가 겹치면 400 이다 — 부분 전송을 막는다")
    void 순서_기존행_충돌() {
        given(stageRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stage(7L, 1)));
        given(stageRepository.findAllByProjectId(PROJECT_ID))
                .willReturn(List.of(stage(7L, 1), stage(8L, 2)));

        assertThatThrownBy(() -> stageCommandService.reorderStages(
                reorder(new ReorderStagesCommand.Item(7L, 2))))
                .isInstanceOf(ValidationException.class);

        Mockito.verify(stageRepository, Mockito.never()).save(any(Stage.class));
    }

    @Test
    @DisplayName("편집 권한이 없으면 수정·순서·삭제 모두 저장까지 가지 않는다")
    void 권한_거부() {
        given(stageRepository.findById(STAGE_ID)).willReturn(Optional.of(stage(STAGE_ID, 1)));
        Mockito.doThrow(new ForbiddenException(ProjectErrorCode.PROJECT_EDIT_DENIED))
                .when(projectAccessUseCase).requireEditable(PROJECT_ID, REQUESTER, "USER");

        assertThatThrownBy(() -> stageCommandService.updateStage(
                new UpdateStageCommand(STAGE_ID, "제안·계약", REQUESTER, "USER")))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> stageCommandService.reorderStages(
                reorder(new ReorderStagesCommand.Item(7L, 1))))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> stageCommandService.deleteStage(
                new DeleteStageCommand(STAGE_ID, 8L, REQUESTER, "USER")))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verify(stageRepository, Mockito.never()).save(any(Stage.class));
        Mockito.verifyNoInteractions(stepRelocationPort, stagePermissionDefaultRepository);
    }

    @Test
    @DisplayName("목록에 없는 스테이지가 섞이면 404 다")
    void 순서_스테이지_없음() {
        given(stageRepository.findAllByIdsInProject(anyCollection(), eq(PROJECT_ID)))
                .willReturn(List.of(stage(7L, 1)));

        assertThatThrownBy(() -> stageCommandService.reorderStages(reorder(
                new ReorderStagesCommand.Item(7L, 1),
                new ReorderStagesCommand.Item(99L, 2))))
                .isInstanceOf(NotFoundException.class);
    }

    // ────────────────────────────── 삭제 ──────────────────────────────

    @Test
    @DisplayName("하위 스텝을 옮기고 스테이지를 논리 삭제한다 — 기본값도 함께 지운다")
    void 삭제() {
        givenStage();
        given(stageRepository.existsInProject(8L, PROJECT_ID)).willReturn(true);
        given(stepRelocationPort.relocateByStage(STAGE_ID, 8L)).willReturn(3);

        StageDeleteResult result = stageCommandService.deleteStage(
                new DeleteStageCommand(STAGE_ID, 8L, REQUESTER, "USER"));

        assertThat(result.movedStepCount()).isEqualTo(3);
        assertThat(result.moveToStageId()).isEqualTo(8L);

        Mockito.verify(stagePermissionDefaultRepository).deleteByStageId(STAGE_ID);
        assertThat(captureSaved().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("moveToStageId 가 0 이면 미소속으로 뺀다")
    void 삭제_미소속_이전() {
        givenStage();
        given(stepRelocationPort.relocateByStage(STAGE_ID, null)).willReturn(2);

        StageDeleteResult result = stageCommandService.deleteStage(
                new DeleteStageCommand(STAGE_ID, 0L, REQUESTER, "USER"));

        assertThat(result.moveToStageId()).isNull();
        Mockito.verify(stageRepository, Mockito.never()).existsInProject(any(), any());
    }

    @Test
    @DisplayName("이전 대상을 안 주면 400 이다 — 스텝은 함께 삭제되지 않는다")
    void 삭제_대상_미지정() {
        givenStage();

        assertThatThrownBy(() -> stageCommandService.deleteStage(
                new DeleteStageCommand(STAGE_ID, null, REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(stepRelocationPort);
    }

    @Test
    @DisplayName("자기 자신으로는 못 옮긴다 — 400")
    void 삭제_자기자신() {
        givenStage();

        assertThatThrownBy(() -> stageCommandService.deleteStage(
                new DeleteStageCommand(STAGE_ID, STAGE_ID, REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("남의 프로젝트 스테이지로는 못 옮긴다 — 400")
    void 삭제_남의_프로젝트() {
        givenStage();
        given(stageRepository.existsInProject(99L, PROJECT_ID)).willReturn(false);

        assertThatThrownBy(() -> stageCommandService.deleteStage(
                new DeleteStageCommand(STAGE_ID, 99L, REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);
    }

    // ────────────────────────────── 헬퍼 ──────────────────────────────

    private ReorderStagesCommand reorder(ReorderStagesCommand.Item... items) {
        return new ReorderStagesCommand(PROJECT_ID, List.of(items), REQUESTER, "USER");
    }

    private Stage stage(Long stageId, int sortOrder) {
        return Stage.restore(stageId, PROJECT_ID, "스테이지 " + stageId, sortOrder,
                LocalDateTime.of(2026, 8, 1, 9, 0), null);
    }

    private void givenStage() {
        given(stageRepository.findById(STAGE_ID)).willReturn(Optional.of(stage(STAGE_ID, 1)));
        Mockito.lenient().when(stageRepository.save(any(Stage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Stage captureSaved() {
        ArgumentCaptor<Stage> captor = ArgumentCaptor.forClass(Stage.class);
        Mockito.verify(stageRepository).save(captor.capture());
        return captor.getValue();
    }
}
