package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.stage.application.command.CreateStageCommand;
import com.group3.vitamins.project.stage.application.command.DeleteStageCommand;
import com.group3.vitamins.project.stage.application.command.ReorderStagesCommand;
import com.group3.vitamins.project.stage.application.command.UpdateStageCommand;
import com.group3.vitamins.project.stage.application.port.StepRelocationPort;
import com.group3.vitamins.project.stage.application.result.StageDeleteResult;
import com.group3.vitamins.project.stage.application.result.StageOrderResult;
import com.group3.vitamins.project.stage.application.result.StageResult;
import com.group3.vitamins.project.stage.application.usecase.StageCommandUseCase;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StageCommandService implements StageCommandUseCase {

    private static final int FIRST_SORT_ORDER = 1;
    private static final long UNASSIGNED = 0L;

    private final StageRepository stageRepository;
    private final StagePermissionDefaultRepository stagePermissionDefaultRepository;
    private final StepRelocationPort stepRelocationPort;
    private final ProjectAccessUseCase projectAccessUseCase;

    @Override
    public StageResult createStage(CreateStageCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        int sortOrder = command.sortOrder() != null
                ? command.sortOrder()
                : nextSortOrder(command.projectId());

        Stage saved = stageRepository.save(Stage.create(
                command.projectId(), command.name(), sortOrder, LocalDateTime.now()));

        return new StageResult(saved.getStageId(), saved.getProjectId(),
                saved.getName(), saved.getSortOrder());
    }

    /** 이름만 바꾼다 (STG-001). 순서는 순서 변경 API 소관이다. */
    @Override
    public StageResult updateStage(UpdateStageCommand command) {
        Stage stage = requireEditableStage(
                command.stageId(), command.requesterUserId(), command.role());

        Stage updated = stageRepository.save(stage.rename(command.name()));

        return new StageResult(updated.getStageId(), updated.getProjectId(),
                updated.getName(), updated.getSortOrder());
    }

    /**
     * 사이드바 순서를 통째로 확정한다 (STG-002).
     * ⛔ 하위 스텝의 sort_order 는 건드리지 않는다 — 스테이지 순서와 스텝 순서는 별개 축이다.
     */
    @Override
    public List<StageOrderResult> reorderStages(ReorderStagesCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        validateNoDuplicates(command.items());
        Map<Long, Stage> stages = loadStages(command.projectId(), command.items());
        validateNoConflictWithUnlisted(command.projectId(), command.items());

        return command.items().stream()
                .map(item -> stageRepository.save(
                        stages.get(item.stageId()).moveTo(item.sortOrder())))
                .map(saved -> new StageOrderResult(saved.getStageId(), saved.getSortOrder()))
                .toList();
    }

    /**
     * 스테이지를 논리 삭제한다 (STG-003). 하위 스텝은 <b>함께 지우지 않고 이전</b>한다.
     * moveToStageId 가 0 이면 미소속(stage_id = NULL)으로 뺀다.
     *
     * <p>⚠️ 스텝을 먼저 옮기고 스테이지를 지운다. 순서를 뒤집으면 스텝이 삭제된 스테이지를 가리킨 채 남는다.
     * 옮겨진 스텝의 <b>권한은 그대로다</b> — 위치와 권한은 독립이다 (INV-01).
     */
    @Override
    public StageDeleteResult deleteStage(DeleteStageCommand command) {
        Stage stage = requireEditableStage(
                command.stageId(), command.requesterUserId(), command.role());

        Long moveToStageId = resolveMoveTarget(command.moveToStageId(), stage);
        int movedStepCount = stepRelocationPort.relocateByStage(
                stage.getStageId(), moveToStageId);

        stagePermissionDefaultRepository.deleteByStageId(stage.getStageId());
        stageRepository.save(stage.delete(LocalDateTime.now()));

        return new StageDeleteResult(stage.getStageId(), movedStepCount, moveToStageId);
    }

    /** 스테이지를 찾고 요청자가 그 프로젝트 EDITOR 인지 확인한다. */
    private Stage requireEditableStage(Long stageId, String requesterUserId, String role) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new NotFoundException(StageErrorCode.STAGE_NOT_FOUND));

        projectAccessUseCase.requireEditable(stage.getProjectId(), requesterUserId, role);
        return stage;
    }

    /**
     * 이전 대상을 검증하고 0 을 미소속(null)으로 바꾼다.
     * 자기 자신이면 스텝이 사라질 스테이지에 남고, 남의 프로젝트면 스텝이 프로젝트를 넘어간다.
     */
    private Long resolveMoveTarget(Long moveToStageId, Stage stage) {
        if (moveToStageId == null) {
            throw new ValidationException(StageErrorCode.STAGE_MOVE_TARGET_REQUIRED);
        }
        if (moveToStageId == UNASSIGNED) {
            return null;
        }
        if (moveToStageId.equals(stage.getStageId())
                || !stageRepository.existsInProject(moveToStageId, stage.getProjectId())) {
            throw new ValidationException(StageErrorCode.STAGE_MOVE_TARGET_INVALID);
        }
        return moveToStageId;
    }

    /** 같은 스테이지가 두 번 오거나 순서 값이 겹치면 거부한다. */
    private void validateNoDuplicates(List<ReorderStagesCommand.Item> items) {
        long distinctStages = items.stream()
                .map(ReorderStagesCommand.Item::stageId).distinct().count();
        long distinctOrders = items.stream()
                .map(ReorderStagesCommand.Item::sortOrder).distinct().count();

        if (distinctStages != items.size() || distinctOrders != items.size()) {
            throw new ValidationException(StageErrorCode.STAGE_ORDER_INVALID);
        }
    }

    /**
     * 요청에 없는 기존 스테이지와 순서 값이 겹치면 거부한다.
     *
     * <p>⚠️ 요청 안의 중복만 보면 부분 전송을 막지 못한다. {@code A=1, B=2} 인 상태에서 {@code A=2} 만
     * 보내면 요청 안에는 중복이 없지만 저장 후 A·B 가 모두 2 가 된다. sort_order 에 UNIQUE 제약이
     * 없어 그대로 저장되고, 조회는 순서만 정렬하므로 화면 순서가 매번 달라진다.
     */
    private void validateNoConflictWithUnlisted(Long projectId,
                                                List<ReorderStagesCommand.Item> items) {
        Set<Long> requestedIds = items.stream()
                .map(ReorderStagesCommand.Item::stageId).collect(Collectors.toSet());
        Set<Integer> requestedOrders = items.stream()
                .map(ReorderStagesCommand.Item::sortOrder).collect(Collectors.toSet());

        boolean conflict = stageRepository.findAllByProjectId(projectId).stream()
                .filter(stage -> !requestedIds.contains(stage.getStageId()))
                .anyMatch(stage -> requestedOrders.contains(stage.getSortOrder()));

        if (conflict) {
            throw new ValidationException(StageErrorCode.STAGE_ORDER_INVALID);
        }
    }

    /** 요청한 스테이지를 한 번에 읽는다. 하나라도 없거나 남의 프로젝트 소속이면 404. */
    private Map<Long, Stage> loadStages(Long projectId, List<ReorderStagesCommand.Item> items) {
        List<Long> stageIds = items.stream()
                .map(ReorderStagesCommand.Item::stageId).toList();

        Map<Long, Stage> found = stageRepository.findAllByIdsInProject(stageIds, projectId).stream()
                .collect(Collectors.toMap(Stage::getStageId, stage -> stage));

        if (found.size() != stageIds.size()) {
            throw new NotFoundException(StageErrorCode.STAGE_NOT_FOUND);
        }
        return found;
    }

    /** sortOrder 미지정 시 max+1 을 쓴다. 스테이지가 없으면 1 부터 시작한다. */
    private int nextSortOrder(Long projectId) {
        return stageRepository.findMaxSortOrder(projectId)
                .map(max -> max + 1)
                .orElse(FIRST_SORT_ORDER);
    }
}
