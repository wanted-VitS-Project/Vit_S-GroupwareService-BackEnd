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

        // 포트 반환형 변경(2026-08-11, DELETE.md D-6)에 맞춘 호출만 바꿨다 — 판정 동작은 그대로다.
        //
        // ⚠️ 이슈 도메인 확인 필요: findRefsByUserIds 는 삭제된 사원(deleted_at IS NOT NULL)도 반환하므로
        //    이 존재 검증을 그대로 통과한다 = 삭제된 사원을 이슈 담당자로 배정할 수 있다.
        //    막으려면 아래 refs 값의 deleted() 를 보고 거부하거나, 검증 전용인
        //    EmployeeLookupPort.findNameByUserId(deleted_at IS NULL 을 본다)로 바꿔야 한다.
        //    남의 도메인 판단이라 동작을 바꾸지 않고 남겨둔다.
        Map<String, EmployeeLookupPort.EmployeeRef> refs =
                employeeLookupPort.findRefsByUserIds(userIds);
        if (refs.size() != userIds.size()) {
            throw new NotFoundException(IssueErrorCode.ISS_ASSIGNEE_NOT_FOUND);
        }

        for (String userId : userIds) {
            requireProjectParticipant(projectId, userId);
        }

        return userIds.stream()
                .map(userId -> new AssigneeView(userId, refs.get(userId).name()))
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
