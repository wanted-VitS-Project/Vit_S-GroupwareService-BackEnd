package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.command.RevokeStepPermissionCommand;
import com.group3.vitamins.project.step.application.command.SetStepPermissionCommand;
import com.group3.vitamins.project.step.application.port.ProjectMemberLookupPort;
import com.group3.vitamins.project.step.application.query.StepPermissionListQuery;
import com.group3.vitamins.project.step.application.result.StepPermissionResult;
import com.group3.vitamins.project.step.application.result.StepPermissionSummary;
import com.group3.vitamins.project.step.application.usecase.StepPermissionUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StepPermissionService implements StepPermissionUseCase {

    private final StepRepository stepRepository;
    private final StepPermissionRepository stepPermissionRepository;
    private final EmployeeLookupPort employeeLookupPort;
    private final ProjectMemberLookupPort projectMemberLookupPort;
    private final ProjectAccessUseCase projectAccessUseCase;

    /**
     * 참여자 전원의 스텝 권한 판정을 돌려준다.
     *
     * <p>여기서 계산하는 값은 <b>대상자</b> 기준이라 {@code StepAccessPolicy} 를 쓰지 않는다 —
     * 그 정책은 요청자의 전역 역할(MASTER·ADMIN)을 함께 보는데, 참여자 목록에는 각자의 전역 역할이 없다.
     * 오버라이드 우선, 없으면 프로젝트 권한 상속이라는 STP-011 규칙만 적용한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<StepPermissionSummary> getPermissions(StepPermissionListQuery query) {
        Step step = requireEditableStep(query.stepId(), query.requesterUserId(), query.role());

        Map<String, MemberPermission> overrides =
                stepPermissionRepository.findAllByStepId(step.getStepId());

        return projectMemberLookupPort
                .findMembers(step.getProjectId(), query.requesterUserId(), query.role())
                .stream()
                .map(member -> {
                    MemberPermission override = overrides.get(member.userId());
                    return new StepPermissionSummary(
                            member.userId(), member.name(),
                            (override != null ? override : member.permission()).name(),
                            override != null);
                })
                .toList();
    }

    /**
     * 오버라이드를 만들거나 바꾼다 (STP-010).
     * 특정 스텝만 가리려면 NONE 행을 명시적으로 넣어야 한다 — 행이 없는 것은 차단이 아니라 상속이다.
     */
    @Override
    public StepPermissionResult setPermission(SetStepPermissionCommand command) {
        Step step = requireEditableStep(
                command.stepId(), command.requesterUserId(), command.role());

        checkNotSelf(command.userId(), command.requesterUserId());
        MemberPermission permission = parsePermission(command.permission());
        checkUserExists(command.userId());

        stepPermissionRepository.save(
                step.getStepId(), command.userId(), permission, LocalDateTime.now());

        return new StepPermissionResult(
                step.getStepId(), command.userId(), permission.name(), true);
    }

    /** 오버라이드 행을 지운다. 지우고 나면 프로젝트 권한을 상속하므로 그 등급을 응답에 담는다 (STP-011). */
    @Override
    public StepPermissionResult revokePermission(RevokeStepPermissionCommand command) {
        Step step = requireEditableStep(
                command.stepId(), command.requesterUserId(), command.role());

        if (!stepPermissionRepository.deleteByStepIdAndUserId(step.getStepId(), command.userId())) {
            throw new NotFoundException(StepErrorCode.STEP_PERMISSION_NOT_FOUND);
        }

        MemberPermission inherited = projectMemberLookupPort
                .findMember(step.getProjectId(), command.userId(),
                        command.requesterUserId(), command.role())
                .map(ProjectMemberLookupPort.Member::permission)
                .orElse(MemberPermission.NONE);

        return new StepPermissionResult(
                step.getStepId(), command.userId(), inherited.name(), false);
    }

    /**
     * 스테이지 하위 스텝 전부에 같은 권한을 찍는다 (STG-004).
     * 스테이지에 스텝이 없으면 0 을 돌려준다 — 그래도 기본값은 저장되므로 앞으로 만들 스텝에는 적용된다.
     */
    @Override
    public int applyToStage(Long stageId, String userId, MemberPermission permission) {
        List<Step> steps = stepRepository.findAllByStageId(stageId);

        LocalDateTime now = LocalDateTime.now();
        steps.forEach(step ->
                stepPermissionRepository.save(step.getStepId(), userId, permission, now));

        return steps.size();
    }

    /**
     * 스텝을 찾고 요청자가 그 프로젝트 EDITOR 인지 확인한다.
     * 권한 관리는 스텝 오버라이드가 아니라 <b>프로젝트</b> 권한으로 판정한다 —
     * 스텝 오버라이드로 스텝 권한을 바꿀 수 있으면 스스로 권한을 키울 수 있다.
     */
    private Step requireEditableStep(Long stepId, String requesterUserId, String role) {
        Step step = stepRepository.findById(stepId)
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));

        projectAccessUseCase.requireEditable(step.getProjectId(), requesterUserId, role);
        return step;
    }

    /** 자기 자신의 권한 행은 못 건드린다 (INV-10). */
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
