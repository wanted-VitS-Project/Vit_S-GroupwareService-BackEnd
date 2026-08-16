package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.policy.StepAccessPolicy;
import com.group3.vitamins.project.step.application.port.StepAccessQueryPort;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StepAccessService implements StepAccessUseCase {

    private final StepAccessQueryPort stepAccessQueryPort;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final StepAccessPolicy stepAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public StepAccessView requireAccess(Long stepId, String requesterUserId, String role) {
        StepAccessQueryPort.StepAccessSnapshot snapshot = findSnapshot(stepId, requesterUserId);
        MemberPermission permission = stepAccessPolicy.requireAccess(
                role, projectPermission(snapshot, role), snapshot.overridePermission());

        return new StepAccessView(snapshot.stepId(), snapshot.projectId(), permission);
    }

    @Override
    public StepAccessView requireEditable(Long stepId, String requesterUserId, String role) {
        StepAccessQueryPort.StepAccessSnapshot snapshot = findSnapshot(stepId, requesterUserId);
        MemberPermission permission = stepAccessPolicy.requireEditable(
                role, projectPermission(snapshot, role), snapshot.overridePermission());

        return new StepAccessView(snapshot.stepId(), snapshot.projectId(), permission);
    }

    /** 스텝이 없거나 논리 삭제됐으면 404. 회사 경계·미참여는 여기서 걸리지 않는다 — 그건 403 이다. */
    private StepAccessQueryPort.StepAccessSnapshot findSnapshot(Long stepId, String requesterUserId) {
        return stepAccessQueryPort
                .findAccess(stepId, requesterUserId, currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(StepErrorCode.STEP_NOT_FOUND));
    }

    /**
     * 프로젝트 권한은 예외를 던지지 않는 판정을 쓴다 — 스텝 엔드포인트는 프로젝트 에러코드를 내리지 않는다.
     *
     * <p>다른 회사이거나 삭제된 프로젝트면 {@code projectVisible} 이 {@code false} 이고,
     * 그때는 role 승격도 보지 않고 {@code NONE} 이다 (기존 {@code ProjectAccessService#resolvePermission}
     * 의 "존재하지 않으면 NONE" 분기와 같은 순서).
     */
    private MemberPermission projectPermission(StepAccessQueryPort.StepAccessSnapshot snapshot,
                                               String role) {
        if (!snapshot.projectVisible()) {
            return MemberPermission.NONE;
        }
        return projectAccessPolicy.resolvePermissionOrNone(role, snapshot.memberPermission());
    }
}
