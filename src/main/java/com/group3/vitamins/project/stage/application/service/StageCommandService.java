package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
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
import com.group3.vitamins.project.stage.application.usecase.StageCascadeUseCase;
import com.group3.vitamins.project.stage.application.usecase.StageCloneUseCase;
import com.group3.vitamins.project.stage.application.usecase.StageCommandUseCase;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StageCommandService
        implements StageCommandUseCase, StageCascadeUseCase, StageCloneUseCase {

    private static final int FIRST_SORT_ORDER = 1;
    private static final long UNASSIGNED = 0L;

    private final StageRepository stageRepository;
    private final StagePermissionDefaultRepository stagePermissionDefaultRepository;
    private final StepRelocationPort stepRelocationPort;
    private final ProjectAccessUseCase projectAccessUseCase;

    //stage 생성
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
                saved.getName(), saved.getSortOrder(), saved.getVersion());
    }

    //stage 수정
    @Override
    public StageResult updateStage(UpdateStageCommand command) {
        //수정권한 확인
        Stage stage = requireEditableStage(
                command.stageId(), command.requesterUserId(), command.role());

        //버전 overwrite 시, 그냥 진입 시, 나누어 져서 진행
        int expected = command.overwrite() ? stage.getVersion() : command.version();

        int updated = stageRepository.renameIfVersionMatches(
                command.stageId(), command.name(), expected);

        if (updated == 0) {
            throw new ConflictException(StageErrorCode.STAGE_VERSION_CONFLICT);
        }

        return new StageResult(stage.getStageId(), stage.getProjectId(),
                command.name(), stage.getSortOrder(), expected + 1);
    }

    /**
     * 사이드바 순서를 통째로 확정한다 (STG-002).
     * ⛔ 하위 스텝의 sort_order 는 건드리지 않는다 — 스테이지 순서와 스텝 순서는 별개 축이다.
     *
     * <p>항목마다 개별 version 을 검사하고 <b>하나라도 어긋나면 요청 전체를 롤백한다</b>
     * (`.ai/docs/global/CONCURRENCY.md` §4-2). 순서 변경은 요청에 목록 전체가 실려 오므로
     * A·B 가 서로 <b>다른</b> 스테이지를 옮겨도 나중 요청이 앞 요청을 되돌린다 — 그래서 가장 위험하다.
     */
    @Override
    public List<StageOrderResult> reorderStages(ReorderStagesCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        validateNoDuplicates(command.items());
        Map<Long, Stage> stages = loadStages(command.projectId(), command.items());
        validateNoConflictWithUnlisted(command.projectId(), command.items());

        List<StageOrderResult> results = new ArrayList<>(command.items().size());

        for (ReorderStagesCommand.Item item : command.items()) {
            Stage moved = stages.get(item.stageId()).moveTo(item.sortOrder());

            int updated = stageRepository.moveIfVersionMatches(
                    moved.getStageId(), moved.getSortOrder(), item.version());

            // ⚠️ 이 예외를 잡아서 넘기면 앞서 갱신한 항목만 커밋되어 순서가 반쯤 바뀐 채 남는다.
            //    ConflictException 은 런타임 예외라 여기서 던져야 트랜잭션 전체가 롤백된다 (§4-3).
            if (updated == 0) {
                throw new ConflictException(StageErrorCode.STAGE_VERSION_CONFLICT);
            }

            results.add(new StageOrderResult(
                    moved.getStageId(), moved.getSortOrder(), item.version() + 1));
        }

        return results;
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

    /**
     * 프로젝트 삭제가 부르는 스테이지 정리 (PRJ-014). 권한은 호출자가 이미 판정했다.
     *
     * <p>⚠️ 기본값을 먼저 지운다. {@code stage_permission_default} 는 하드 삭제인데
     * 스테이지를 먼저 죽여도 행은 그대로 남으므로, 순서를 뒤집으면 지울 근거를 잃는다.
     */
    @Override
    public int deleteByProjectId(Long projectId) {
        stagePermissionDefaultRepository.deleteByProjectId(projectId);
        return stageRepository.deleteByProjectId(projectId, LocalDateTime.now());
    }

    /**
     * 프로젝트 복제가 부르는 스테이지 복사 (PRJ-018). 권한은 호출자가 원본 참여자로 이미 판정했다.
     *
     * <p>이름과 {@code sortOrder} 만 옮긴다. {@code stage_permission_default}(새 스텝 권한 기본값)는
     * 복사하지 않는다 — 참여자를 복제하지 않으므로 복제본에서는 <b>아무도 가리키지 않는 죽은 행</b>이 된다.
     *
     * <p>⚠️ {@code sortOrder} 는 원본 값 그대로다. 다시 매기면 스테이지 순서가 원본과 달라진다.
     */
    @Override
    public Map<Long, Long> cloneToProject(Long sourceProjectId, Long targetProjectId) {
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Long> stageIdMap = new LinkedHashMap<>();
        for (Stage source : stageRepository.findAllByProjectId(sourceProjectId)) {
            Stage clone = stageRepository.save(Stage.create(
                    targetProjectId, source.getName(), source.getSortOrder(), now));

            stageIdMap.put(source.getStageId(), clone.getStageId());
        }
        return stageIdMap;
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
