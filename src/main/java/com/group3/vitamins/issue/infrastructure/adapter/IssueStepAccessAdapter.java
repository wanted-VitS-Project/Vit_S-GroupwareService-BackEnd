package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueStepAccessAdapter implements IssueStepAccessPort {

    private final StepAccessUseCase stepAccessUseCase;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final IssueQueryPort issueQueryPort;

    /** 조회는 스텝 오버라이드를 보지 않는다 — 프로젝트 권한(VIEWER 이상)만 있으면 그 프로젝트의 모든 스텝을 조회할 수 있다. */
    @Override
    public StepAccessView requireAccess(Long stepId, String requesterUserId, String role) {
        Long projectId = findProjectId(stepId, StepErrorCode.STEP_NOT_FOUND);
        if (isBlocked(projectId, requesterUserId, role)) {
            throw new ForbiddenException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
        return new StepAccessView(stepId, projectId);
    }

    @Override
    public StepAccessView requireIssueAccess(Long stepId, String requesterUserId, String role) {
        Long projectId = findProjectId(stepId, IssueErrorCode.ISS_NOT_FOUND);
        if (isBlocked(projectId, requesterUserId, role)) {
            throw new ForbiddenException(IssueErrorCode.ISS_ACCESS_PERMISSION_REQUIRED);
        }
        return new StepAccessView(stepId, projectId);
    }

    private Long findProjectId(Long stepId, ErrorCode notFoundCode) {
        return issueQueryPort.findProjectId(stepId)
                .orElseThrow(() -> new NotFoundException(notFoundCode));
    }

    /** 예외를 던지지 않는 판정을 쓴다 — Issue 엔드포인트마다 에러코드가 달라 여기서 감싼다. */
    private boolean isBlocked(Long projectId, String requesterUserId, String role) {
        MemberPermission permission = projectAccessUseCase.resolvePermission(projectId, requesterUserId, role);
        return permission == null || permission == MemberPermission.NONE;
    }

    /**
     * 수정·생성·삭제는 여전히 해당 스텝의 EDITOR 권한이 필요하다 — 스텝 오버라이드 경로를 그대로 쓴다.
     *
     * <p>projectId 를 따로 조회하지 않는다 — 스텝 판정이 이미 그 값을 돌려주기 때문이다.
     * 스텝이 없을 때 나가는 에러코드는 아래 {@code catch} 가 {@code ISS_STEP_NOT_FOUND} 로 바꿔주므로
     * 선조회를 없애도 응답은 같다.
     */
    @Override
    public StepAccessView requireEditable(Long stepId, String requesterUserId, String role) {
        try {
            StepAccessUseCase.StepAccessView step =
                    stepAccessUseCase.requireEditable(stepId, requesterUserId, role);
            return new StepAccessView(stepId, step.projectId());
        } catch (NotFoundException e) {
            throw new NotFoundException(IssueErrorCode.ISS_STEP_NOT_FOUND, e);
        } catch (ForbiddenException e) {
            throw new ForbiddenException(IssueErrorCode.ISS_EDIT_PERMISSION_REQUIRED, e);
        }
    }

    /** 프로젝트 스코프 엔드포인트라 Step 존재 확인을 거치지 않는다. Project 존재·권한 에러코드를 그대로 낸다. */
    @Override
    public void requireProjectAccess(Long projectId, String requesterUserId, String role) {
        projectAccessUseCase.requireAccess(projectId, requesterUserId, role);
    }
}
