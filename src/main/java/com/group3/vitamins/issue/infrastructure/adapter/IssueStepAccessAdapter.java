package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueStepAccessAdapter implements IssueStepAccessPort {

    private final StepAccessUseCase stepAccessUseCase;

    @Override
    public StepAccessView requireEditable(Long stepId, String requesterUserId, String role) {
        try {
            StepAccessUseCase.StepAccessView step =
                    stepAccessUseCase.requireEditable(stepId, requesterUserId, role);
            return new StepAccessView(step.stepId(), step.projectId());
        } catch (NotFoundException e) {
            throw new NotFoundException(IssueErrorCode.ISS_STEP_NOT_FOUND, e);
        } catch (ForbiddenException e) {
            throw new ForbiddenException(IssueErrorCode.ISS_EDIT_PERMISSION_REQUIRED, e);
        }
    }
}
