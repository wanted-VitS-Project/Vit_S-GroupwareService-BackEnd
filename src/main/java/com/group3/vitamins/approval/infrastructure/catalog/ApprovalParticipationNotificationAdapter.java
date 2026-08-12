package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.ApprovalParticipationNotificationPort;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalParticipationNotificationMapper;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalParticipationNotificationRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalStepEditorRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/** 결재·프로젝트·권한·사원 테이블 조인 조회를 애플리케이션 포트로 변환한다. */
@Component
@RequiredArgsConstructor
public class ApprovalParticipationNotificationAdapter implements ApprovalParticipationNotificationPort {

    private final ApprovalParticipationNotificationMapper mapper;

    @Override
    public List<Target> findPendingApproverTargets(String userId, Long companyId) {
        return mapper.findPendingApproverTargets(userId, companyId).stream()
                .map(this::toTarget)
                .toList();
    }

    @Override
    public List<Target> findDrafterTargets(String userId, Long companyId) {
        return mapper.findDrafterTargets(userId, companyId).stream()
                .map(this::toTarget)
                .toList();
    }

    @Override
    public List<Editor> findActiveStepEditors(Collection<Long> blockIds, Long companyId) {
        if (blockIds.isEmpty()) {
            return List.of();
        }
        return mapper.findActiveStepEditors(blockIds, companyId).stream()
                .map(this::toEditor)
                .toList();
    }

    private Target toTarget(ApprovalParticipationNotificationRow row) {
        return new Target(row.approvalId(), row.revisionId(), row.blockId(), row.title(),
                row.drafterId(), row.actingDrafterId());
    }

    private Editor toEditor(ApprovalStepEditorRow row) {
        return new Editor(row.blockId(), row.userId());
    }
}
