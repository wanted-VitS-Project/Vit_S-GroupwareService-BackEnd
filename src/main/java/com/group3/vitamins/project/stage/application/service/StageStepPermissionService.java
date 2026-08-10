package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.command.ApplyStagePermissionCommand;
import com.group3.vitamins.project.stage.application.port.StepPermissionBulkPort;
import com.group3.vitamins.project.stage.application.result.StageStepPermissionResult;
import com.group3.vitamins.project.stage.application.usecase.StageStepPermissionUseCase;
import com.group3.vitamins.project.stage.domain.exception.StageErrorCode;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Transactional
public class StageStepPermissionService implements StageStepPermissionUseCase {

    private final StageRepository stageRepository;
    private final StagePermissionDefaultRepository stagePermissionDefaultRepository;
    private final StepPermissionBulkPort stepPermissionBulkPort;
    private final EmployeeLookupPort employeeLookupPort;
    private final ProjectAccessUseCase projectAccessUseCase;

    /**
     * 두 가지 일을 한다 (STG-004).
     * ① 기본값 저장 — 앞으로 이 스테이지에 만들어지는 스텝에 자동 적용된다.
     * ② 기존 스텝 전개 — {@code applyToExistingSteps} 가 true 일 때만.
     *
     * <p>⚠️ 기본값은 <b>판정에 쓰이지 않는다</b> (INV-01). 스텝 생성 시 step_permission 으로 복사될 뿐이라,
     * 스텝을 다른 스테이지로 옮겨도 권한이 따라 바뀌지 않는다.
     */
    @Override
    public StageStepPermissionResult apply(ApplyStagePermissionCommand command) {
        Stage stage = stageRepository.findById(command.stageId())
                .orElseThrow(() -> new NotFoundException(StageErrorCode.STAGE_NOT_FOUND));

        projectAccessUseCase.requireEditable(
                stage.getProjectId(), command.requesterUserId(), command.role());

        checkNotSelf(command.userId(), command.requesterUserId());
        MemberPermission permission = parsePermission(command.permission());
        checkUserExists(command.userId());

        stagePermissionDefaultRepository.save(
                stage.getStageId(), command.userId(), permission, LocalDateTime.now());

        int appliedStepCount = command.applyToExistingSteps()
                ? stepPermissionBulkPort.applyToStage(
                        stage.getStageId(), command.userId(), permission)
                : 0;

        return new StageStepPermissionResult(
                stage.getStageId(), command.userId(), permission.name(), appliedStepCount);
    }

    /**
     * 자기 자신의 권한은 못 건드린다 (INV-10).
     * 명세 표에는 없지만 막는다 — 안 막으면 단건 권한 부여의 INV-10 을 일괄로 우회할 수 있다.
     */
    private void checkNotSelf(String targetUserId, String requesterUserId) {
        if (targetUserId.equals(requesterUserId)) {
            throw new ForbiddenException(ProjectErrorCode.MEMBER_SELF_EDIT_DENIED);
        }
    }

    /** VIEWER · EDITOR · NONE 만 받는다. null 이어도 400 이 되도록 valueOf 를 쓰지 않는다. */
    private MemberPermission parsePermission(String raw) {
        return Arrays.stream(MemberPermission.values())
                .filter(value -> value.name().equals(raw))
                .findFirst()
                .orElseThrow(() -> new ValidationException(StepErrorCode.STEP_PERMISSION_INVALID));
    }

    /** 참여자 여부는 보지 않는다 — 명세에 해당 에러코드가 없다. 사원 존재만 확인한다. */
    private void checkUserExists(String userId) {
        if (employeeLookupPort.findNameByUserId(userId) == null) {
            throw new NotFoundException(ProjectErrorCode.USER_NOT_FOUND);
        }
    }
}
