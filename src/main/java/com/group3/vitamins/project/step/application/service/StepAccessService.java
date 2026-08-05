package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.policy.StepAccessPolicy;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StepAccessService implements StepAccessUseCase {

    private final StepRepository stepRepository;
    private final StepPermissionRepository stepPermissionRepository;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final StepAccessPolicy stepAccessPolicy;

    @Override
    public StepAccessView requireAccess(Long stepId, String requesterUserId, String role) {
        Step step = findStep(stepId);
        MemberPermission permission = stepAccessPolicy.requireAccess(
                role, projectPermission(step, requesterUserId, role),
                override(stepId, requesterUserId));

        return new StepAccessView(step.getStepId(), step.getProjectId(), permission);
    }

    @Override
    public StepAccessView requireEditable(Long stepId, String requesterUserId, String role) {
        Step step = findStep(stepId);
        MemberPermission permission = stepAccessPolicy.requireEditable(
                role, projectPermission(step, requesterUserId, role),
                override(stepId, requesterUserId));

        return new StepAccessView(step.getStepId(), step.getProjectId(), permission);
    }

    private Step findStep(Long stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));
    }

    /** 프로젝트 권한은 예외를 던지지 않는 판정을 쓴다 — 스텝 엔드포인트는 프로젝트 에러코드를 내리지 않는다. */
    private MemberPermission projectPermission(Step step, String requesterUserId, String role) {
        return projectAccessUseCase.resolvePermission(step.getProjectId(), requesterUserId, role);
    }

    private MemberPermission override(Long stepId, String requesterUserId) {
        return stepPermissionRepository.findOverride(stepId, requesterUserId)
                .orElse(null);
    }
}