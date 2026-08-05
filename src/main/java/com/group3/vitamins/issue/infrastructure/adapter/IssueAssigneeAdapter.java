package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueAssigneePort;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
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
    private final ProjectAccessUseCase projectAccessUseCase;

    @Override
    public List<AssigneeView> validateAssignable(Long projectId, List<String> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        Map<String, String> names = employeeLookupPort.findNamesByUserIds(userIds);
        if (names.size() != userIds.size()) {
            throw new NotFoundException(IssueErrorCode.ISS_ASSIGNEE_NOT_FOUND);
        }

        for (String userId : userIds) {
            requireProjectParticipant(projectId, userId);
        }

        return userIds.stream()
                .map(userId -> new AssigneeView(userId, names.get(userId)))
                .toList();
    }

    /**
     * ASN-003(담당자는 프로젝트 참여자여야 한다)은 스텝 단위 오버라이드와 무관하다 — 그래서 스텝 접근
     * 여부(StepAccessUseCase)가 아니라 프로젝트 참여 여부(ProjectAccessUseCase)만 본다. 프로젝트 도메인이
     * 이미 노출한 인바운드 유스케이스를 재사용해 project_member 조회 로직을 복제하지 않는다.
     */
    private void requireProjectParticipant(Long projectId, String userId) {
        MemberPermission permission = projectAccessUseCase.resolvePermission(projectId, userId, "");
        if (permission == MemberPermission.NONE) {
            throw new ValidationException(IssueErrorCode.ISS_ASSIGNEE_NOT_PROJECT_MEMBER);
        }
    }
}
