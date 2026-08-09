package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import com.group3.vitamins.project.step.application.command.UpdateStepCommand;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import com.group3.vitamins.project.step.application.result.StepResult;
import com.group3.vitamins.project.step.application.result.StepUpdateResult;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.application.usecase.StepCommandUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class StepCommandService implements StepCommandUseCase {

    private static final int FIRST_SORT_ORDER = 1;

    private final StepRepository stepRepository;
    private final StageLookupPort stageLookupPort;
    private final EmployeeLookupPort employeeLookupPort;
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
        StepResult.Owner owner = resolveOwner(command.ownerUserId());

        Step saved = stepRepository.save(Step.create(
                command.projectId(), command.stageId(), command.name(),
                nextSortOrder(command.projectId()),
                command.startedOn(), command.endedOn(), command.ownerUserId(),
                LocalDateTime.now()));

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
        StepResult.Owner owner = resolveOwner(command.ownerUserId());

        Step updated = stepRepository.save(step.update(
                command.name(), command.startedOn(), command.endedOn(),
                command.ownerUserId(), LocalDateTime.now()));

        return new StepUpdateResult(updated.getStepId(), updated.getStageId(), updated.getName(),
                updated.getStartedOn(), updated.getEndedOn(), owner, updated.getUpdatedAt());
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

    /** 책임자 사번을 보냈으면 존재를 확인하고 이름을 함께 돌려준다. 안 보냈으면 null(해제). */
    private StepResult.Owner resolveOwner(String ownerUserId) {
        if (ownerUserId == null || ownerUserId.isBlank()) {
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
