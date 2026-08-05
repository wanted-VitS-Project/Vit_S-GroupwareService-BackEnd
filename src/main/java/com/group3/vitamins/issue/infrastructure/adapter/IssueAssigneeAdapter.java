package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueAssigneePort;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueAssigneeAdapter implements IssueAssigneePort {

    private final EmployeeLookupPort employeeLookupPort;
    private final StepAccessUseCase stepAccessUseCase;

    @Override
    public List<AssigneeView> validateAssignable(Long stepId, List<String> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        Map<String, String> names = employeeLookupPort.findNamesByUserIds(userIds);
        if (names.size() != userIds.size()) {
            throw new NotFoundException(IssueErrorCode.ISS_ASSIGNEE_NOT_FOUND);
        }

        for (String userId : userIds) {
            requireProjectParticipant(stepId, userId);
        }

        return userIds.stream()
                .map(userId -> new AssigneeView(userId, names.get(userId)))
                .toList();
    }

    /** 담당자의 스텝 접근 권한 판정은 스텝 도메인이 노출한 StepAccessUseCase에 위임한다 — 로직을 복제하지 않는다. */
    private void requireProjectParticipant(Long stepId, String userId) {
        try {
            stepAccessUseCase.requireAccess(stepId, userId, "");
        } catch (ForbiddenException | NotFoundException e) {
            throw new ValidationException(IssueErrorCode.ISS_ASSIGNEE_NOT_PROJECT_MEMBER);
        }
    }
}
