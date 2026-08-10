package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.block.domain.exception.BlockErrorCode;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.step.application.command.ChangeStepStatusCommand;
import com.group3.vitamins.project.step.application.command.CompleteStepCommand;
import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import com.group3.vitamins.project.step.application.command.DeleteStepCommand;
import com.group3.vitamins.project.step.application.command.ReorderStepsCommand;
import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import com.group3.vitamins.project.step.application.port.IssueCloseCommandPort;
import com.group3.vitamins.project.step.application.port.IssueDeleteCommandPort;
import com.group3.vitamins.project.step.application.port.IssueStatLookupPort;
import com.group3.vitamins.project.step.application.port.StepBlockCascadePort;
import com.group3.vitamins.project.step.application.port.StagePermissionDefaultLookupPort;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.application.result.StepCompleteResult;
import com.group3.vitamins.project.step.application.result.StepDeleteResult;
import com.group3.vitamins.project.step.application.result.StepOrderResult;
import com.group3.vitamins.project.step.application.result.StepPerson;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.application.result.StepStatusResult;
import com.group3.vitamins.project.step.application.result.StepUpdateResult;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.application.usecase.StepCommandUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.OpenIssueAction;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StepCommandService implements StepCommandUseCase {

    private static final int FIRST_SORT_ORDER = 1;

    private final StepRepository stepRepository;
    private final StageLookupPort stageLookupPort;
    private final EmployeeLookupPort employeeLookupPort;
    private final IssueStatLookupPort issueStatLookupPort;
    private final IssueCloseCommandPort issueCloseCommandPort;
    private final IssueDeleteCommandPort issueDeleteCommandPort;
    private final StepBlockCascadePort stepBlockCascadePort;
    private final StagePermissionDefaultLookupPort stagePermissionDefaultLookupPort;
    private final StepPermissionRepository stepPermissionRepository;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final StepAccessUseCase stepAccessUseCase;

    @Override
    public StepResult createStep(CreateStepCommand command) {
        validateDateRange(command.startedOn(), command.endedOn());
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        if (command.stageId() != null) {
            checkStageInProject(command.stageId(), command.projectId());
        }
        String ownerUserId = normalizeOwnerUserId(command.ownerUserId());
        StepResult.Owner owner = resolveOwner(ownerUserId);

        Step saved = stepRepository.save(Step.create(
                command.projectId(), command.stageId(), command.name(),
                nextSortOrder(command.projectId()),
                command.startedOn(), command.endedOn(), ownerUserId,
                LocalDateTime.now()));

        applyStageDefaults(saved);

        return new StepResult(
                saved.getStepId(), saved.getProjectId(), saved.getStageId(), saved.getName(),
                saved.getStatus().name(), saved.getSortOrder(),
                saved.getStartedOn(), saved.getEndedOn(), owner, saved.getCreatedAt());
    }

    /**
     * 이름·기간·책임자를 덮어쓴다. 소속 스테이지·정렬순서는 건드리지 않는다 —
     * 위치 변경은 순서 변경 API 로 일원화했다(폼에 박힌 옛 위치로 되돌아가는 사고를 막는다).
     *
     * <p>권한은 프로젝트가 아니라 <b>스텝</b> 기준이다 — 오버라이드로 이 스텝만 편집 가능한 사람이 있다.
     */
    @Override
    public StepUpdateResult updateStep(UpdateStepCommand command) {
        stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        Step step = stepRepository.findById(command.stepId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        validateDateRange(command.startedOn(), command.endedOn());
        String ownerUserId = normalizeOwnerUserId(command.ownerUserId());
        StepResult.Owner owner = resolveOwner(ownerUserId);

        Step updated = stepRepository.save(step.update(
                command.name(), command.startedOn(), command.endedOn(),
                ownerUserId, LocalDateTime.now()));

        return new StepUpdateResult(updated.getStepId(), updated.getStageId(), updated.getName(),
                updated.getStartedOn(), updated.getEndedOn(), owner, updated.getUpdatedAt());
    }

    /**
     * 보드 배치를 통째로 확정한다 (STP-002). 목록에 없는 스텝은 손대지 않는다.
     *
     * <p>권한은 <b>프로젝트</b> EDITOR 다 — 스텝 하나가 아니라 보드 전체를 재배치하는 조작이다.
     * 선행 스텝 완료 여부는 검사하지 않는다.
     */
    @Override
    public List<StepOrderResult> reorderSteps(ReorderStepsCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        validateNoDuplicates(command.items());
        Map<Long, Step> steps = loadSteps(command.projectId(), command.items());
        validateNoConflictWithUnlisted(command.projectId(), command.items());
        checkStagesInProject(command.projectId(), command.items());

        LocalDateTime now = LocalDateTime.now();
        return command.items().stream()
                .map(item -> stepRepository.save(
                        steps.get(item.stepId()).moveTo(item.stageId(), item.sortOrder(), now)))
                .map(saved -> new StepOrderResult(
                        saved.getStepId(), saved.getStageId(), saved.getSortOrder()))
                .toList();
    }

    /**
     * 상태를 바꾼다 (STP-004). 진척률과는 별개 값이라 이슈 집계를 보지 않는다.
     * DONE 은 여기서 못 넣는다 — 완료는 미완료 이슈 처리 선택이 필요해 별도 API 다.
     */
    @Override
    public StepStatusResult changeStatus(ChangeStepStatusCommand command) {
        stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        Step step = stepRepository.findById(command.stepId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        Step updated = stepRepository.save(
                step.changeStatus(parseStatus(command.status()), LocalDateTime.now()));

        return new StepStatusResult(
                updated.getStepId(), updated.getStatus().name(), updated.getUpdatedAt());
    }

    /**
     * 완료 처리한다 (STP-005·006). 미완료 이슈가 남아 있어도 완료를 막지 않는다.
     *
     * <p>CLOSE 면 남은 이슈를 먼저 닫고 스텝을 완료한다. 같은 트랜잭션이라 하나라도 실패하면 전부 되돌아간다.
     * 이미 완료된 스텝은 완료자·완료시각을 덮어쓰지 않는다 — 두 번 눌렀다고 기록이 바뀌면 안 된다.
     */
    @Override
    public StepCompleteResult completeStep(CompleteStepCommand command) {
        stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        Step step = stepRepository.findById(command.stepId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        OpenIssueAction action = parseOpenIssueAction(command.openIssueAction());
        List<Long> openIssueIds = issueStatLookupPort.findOpenIssueIds(command.stepId());

        int closedIssueCount = 0;
        if (action == OpenIssueAction.CLOSE) {
            openIssueIds.forEach(issueId -> issueCloseCommandPort.close(
                    issueId, command.requesterUserId(), command.role()));
            closedIssueCount = openIssueIds.size();
        }

        Step completed = step.getStatus() == StepStatus.DONE
                ? step
                : stepRepository.save(
                        step.complete(command.requesterUserId(), LocalDateTime.now()));

        return new StepCompleteResult(
                completed.getStepId(), completed.getStatus().name(),
                openIssueIds.size(), action.name(), closedIssueCount,
                toPerson(completed.getCompletedBy()), completed.getCompletedAt());
    }

    /**
     * 스텝을 논리 삭제한다 (STP-013). 하위 블록·이슈가 함께 삭제된다.
     *
     * <p>⛔ 삭제 잠금은 없다 (2026-08-09 · STP-009 폐기). 살리고 싶은 블록은
     * {@code moveBlockIds} 로 골라 다른 스텝으로 옮긴다 — 그게 잠금을 대신하는 탈출구다.
     * 이슈는 선택지가 없다 (STP-008 폐기).
     *
     * <p>권한은 <b>프로젝트</b> EDITOR 다 (명세 STP-013). 하위 블록·이슈 정리는 권한을 다시 묻지 않는
     * cascade 경로로 부른다 — 스텝 EDITOR 를 재요구하면 이 스텝에 NONE·VIEWER 오버라이드를 가진
     * 프로젝트 EDITOR 가 명세상 허용된 삭제를 403 으로 거부당한다.
     *
     * <p>⚠️ 재무 연결 해제(BLK-013)는 아직 없다. 입출금·계산서가 연결된 정산 블록도 그냥 삭제되며
     * {@code cash_flow.settle_block_id}·{@code tax_invoice.settle_block_id} 가 삭제된 블록을 계속 가리킨다.
     * 옛 {@code payment}·{@code tax_invoice_confirm} 은 정산 재설계에서 테이블째 사라졌다
     * ({@code V20260809130000}). 두 컬럼은 {@code settlement_block} 을 <b>실제 FK 로</b> 참조하므로
     * 보존기간 만료 하드 삭제가 붙으면 연결을 끊기 전까지 정리 자체가 FK 위반으로 실패한다.
     */
    @Override
    public StepDeleteResult deleteStep(DeleteStepCommand command) {
        Step step = stepRepository.findById(command.stepId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        projectAccessUseCase.requireEditable(
                step.getProjectId(), command.requesterUserId(), command.role());

        List<Long> blockIds = stepBlockCascadePort.findBlockIds(step.getStepId());
        List<Long> moveBlockIds = resolveMoveBlockIds(command, step, blockIds);
        List<Long> deleteBlockIds = blockIds.stream()
                .filter(blockId -> !moveBlockIds.contains(blockId))
                .toList();

        if (!moveBlockIds.isEmpty()) {
            stepBlockCascadePort.moveBlocks(moveBlockIds, command.moveToStepId());
        }
        stepBlockCascadePort.deleteBlocks(deleteBlockIds, command.requesterUserId());

        List<Long> issueIds = issueStatLookupPort.findAllIssueIds(step.getStepId());
        issueDeleteCommandPort.delete(issueIds);

        stepRepository.save(step.delete(LocalDateTime.now()));

        return new StepDeleteResult(step.getStepId(),
                moveBlockIds.size(), deleteBlockIds.size(), issueIds.size());
    }

    /**
     * 살릴 블록 목록을 검증한다. 지정이 없으면 전부 삭제라 빈 목록이다.
     *
     * <p>이동 대상은 같은 프로젝트여야 한다 — 다른 프로젝트로 넘기면 입금확인 블록의
     * 회차 번호가 프로젝트 스코프라 충돌한다.
     */
    private List<Long> resolveMoveBlockIds(DeleteStepCommand command, Step step,
                                           List<Long> blockIds) {
        if (command.moveBlockIds() == null || command.moveBlockIds().isEmpty()) {
            return List.of();
        }
        if (command.moveToStepId() == null) {
            throw new ValidationException(BlockErrorCode.BLOCK_MOVE_TARGET_REQUIRED);
        }
        // ⚠️ 같은 프로젝트인지 여기서 봐야 한다. cascade 이동은 권한 판정을 건너뛰므로
        // 예전처럼 stepAccessUseCase 가 두 스텝의 projectId 를 대신 비교해 주지 않는다.
        if (command.moveToStepId().equals(step.getStepId())
                || stepRepository.findById(command.moveToStepId())
                        .filter(target -> target.getProjectId().equals(step.getProjectId()))
                        .isEmpty()) {
            throw new ValidationException(BlockErrorCode.BLOCK_MOVE_TARGET_INVALID);
        }
        if (!blockIds.containsAll(command.moveBlockIds())) {
            throw new NotFoundException(BlockErrorCode.BLOCK_NOT_FOUND);
        }
        return command.moveBlockIds();
    }

    /**
     * NOT_STARTED · IN_PROGRESS 만 받는다. DONE 은 오타와 같은 코드로 거부한다.
     *
     * <p>{@code valueOf} 를 쓰지 않는다 — 값이 null 이면 IllegalArgumentException 이 아니라 NPE 가 나서
     * 400 이 아닌 500 으로 샌다.
     */
    private StepStatus parseStatus(String raw) {
        StepStatus status = Arrays.stream(StepStatus.values())
                .filter(value -> value.name().equals(raw))
                .findFirst()
                .orElseThrow(() -> new ValidationException(StepErrorCode.STEP_STATUS_INVALID));

        if (status == StepStatus.DONE) {
            throw new ValidationException(StepErrorCode.STEP_STATUS_INVALID);
        }
        return status;
    }

    /** 누락은 요청 DTO 가 OPEN_ISSUE_ACTION_REQUIRED 로 잡는다. 여기 오는 건 오타뿐이다. */
    private OpenIssueAction parseOpenIssueAction(String raw) {
        return Arrays.stream(OpenIssueAction.values())
                .filter(value -> value.name().equals(raw))
                .findFirst()
                .orElseThrow(() ->
                        new ValidationException(StepErrorCode.OPEN_ISSUE_ACTION_INVALID));
    }

    /** 완료자 사번에 이름을 붙인다. 완료자는 요청자 본인이라 존재 검증을 따로 하지 않는다. */
    private StepPerson toPerson(String userId) {
        if (userId == null) {
            return null;
        }
        return new StepPerson(userId, employeeLookupPort.findNameByUserId(userId));
    }

    /**
     * 같은 스텝이 두 번 오거나 순서 값이 겹치면 거부한다.
     * 그냥 두면 마지막 값만 남아 조용히 다른 배치가 저장된다.
     */
    private void validateNoDuplicates(List<ReorderStepsCommand.Item> items) {
        long distinctSteps = items.stream()
                .map(ReorderStepsCommand.Item::stepId).distinct().count();
        long distinctOrders = items.stream()
                .map(ReorderStepsCommand.Item::sortOrder).distinct().count();

        if (distinctSteps != items.size() || distinctOrders != items.size()) {
            throw new ValidationException(StepErrorCode.STEP_ORDER_INVALID);
        }
    }

    /**
     * 요청에 없는 기존 스텝과 순서 값이 겹치면 거부한다.
     *
     * <p>⚠️ 요청 안의 중복만 보면 부분 전송을 막지 못한다. sort_order 는 스테이지별이 아니라
     * <b>프로젝트 전체</b> 기준이라, 목록 밖 스텝과 겹치면 보드 순서가 비결정적으로 뒤집힌다.
     */
    private void validateNoConflictWithUnlisted(Long projectId,
                                                List<ReorderStepsCommand.Item> items) {
        Set<Long> requestedIds = items.stream()
                .map(ReorderStepsCommand.Item::stepId).collect(Collectors.toSet());
        Set<Integer> requestedOrders = items.stream()
                .map(ReorderStepsCommand.Item::sortOrder).collect(Collectors.toSet());

        boolean conflict = stepRepository.search(projectId, null, null).stream()
                .filter(step -> !requestedIds.contains(step.getStepId()))
                .anyMatch(step -> requestedOrders.contains(step.getSortOrder()));

        if (conflict) {
            throw new ValidationException(StepErrorCode.STEP_ORDER_INVALID);
        }
    }

    /** 요청한 스텝을 한 번에 읽는다. 하나라도 없거나 남의 프로젝트 소속이면 404. */
    private Map<Long, Step> loadSteps(Long projectId, List<ReorderStepsCommand.Item> items) {
        List<Long> stepIds = items.stream().map(ReorderStepsCommand.Item::stepId).toList();

        Map<Long, Step> found = stepRepository.findAllByIdsInProject(stepIds, projectId).stream()
                .collect(Collectors.toMap(Step::getStepId, step -> step));

        if (found.size() != stepIds.size()) {
            throw new NotFoundException(StepErrorCode.STEP_NOT_FOUND);
        }
        return found;
    }

    /** 이동 대상 스테이지가 전부 이 프로젝트 소속인지 확인한다. null 은 미소속 이동이라 검사 대상이 아니다. */
    private void checkStagesInProject(Long projectId, List<ReorderStepsCommand.Item> items) {
        items.stream()
                .map(ReorderStepsCommand.Item::stageId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(stageId -> checkStageInProject(stageId, projectId));
    }

    /**
     * 시작일·종료일이 둘 다 있을 때만 순서를 검증한다. 둘 다 선택 입력이다.
     *
     * <p>필수·길이 검증은 요청 DTO 의 Bean Validation 이 맡는다 — 여기 남은 건
     * 두 필드의 <b>관계</b>라 필드 단위 애노테이션으로 표현할 수 없는 규칙뿐이다.
     */
    private void validateDateRange(LocalDate startedOn, LocalDate endedOn) {
        if (startedOn != null && endedOn != null && startedOn.isAfter(endedOn)) {
            throw new ValidationException(StepErrorCode.STEP_DATE_RANGE_INVALID);
        }
    }

    /** 지정한 스테이지가 같은 프로젝트 소속인지 확인한다. 남의 프로젝트 스테이지는 못 쓴다. */
    private void checkStageInProject(Long stageId, Long projectId) {
        if (!stageLookupPort.existsInProject(stageId, projectId)) {
            throw new NotFoundException(StageErrorCode.STAGE_NOT_FOUND);
        }
    }

    /**
     * 공백 문자열을 해제(null)로 맞춘다. 저장 값과 응답을 같은 값으로 만들기 위한 것이다 —
     * 정규화 없이 원본을 저장하면 응답은 책임자 없음인데 DB 에는 공백이 남는다.
     */
    /**
     * 소속 스테이지에 걸린 권한 기본값을 이 스텝의 step_permission 행으로 복사한다 (STG-004).
     *
     * <p>⚠️ <b>생성 시점에만</b> 읽는다. 이후 기본값을 고쳐도 이 스텝엔 반영되지 않고,
     * 스텝을 다른 스테이지로 옮겨도 권한이 따라가지 않는다 — 판정은 여전히 step_permission 한 곳이다 (INV-01).
     * 미소속 스텝(stageId == null)은 기본값이 없어 프로젝트 권한을 상속한다.
     */
    private void applyStageDefaults(Step step) {
        if (step.getStageId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        stagePermissionDefaultLookupPort.findDefaults(step.getStageId())
                .forEach((userId, permission) -> stepPermissionRepository.save(
                        step.getStepId(), userId, permission, now));
    }

    private String normalizeOwnerUserId(String ownerUserId) {
        return (ownerUserId == null || ownerUserId.isBlank()) ? null : ownerUserId;
    }

    /** 책임자 사번을 보냈으면 존재를 확인하고 이름을 함께 돌려준다. 안 보냈으면 null(해제). */
    private StepResult.Owner resolveOwner(String ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        String name = employeeLookupPort.findNameByUserId(ownerUserId);
        if (name == null) {
            throw new NotFoundException(ProjectErrorCode.USER_NOT_FOUND);
        }
        return new StepResult.Owner(ownerUserId, name);
    }

    /** 프로젝트 전체 기준 max+1. 스텝이 없으면 1 부터 시작한다. */
    private int nextSortOrder(Long projectId) {
        return stepRepository.findMaxSortOrder(projectId)
                .map(max -> max + 1)
                .orElse(FIRST_SORT_ORDER);
    }
}
