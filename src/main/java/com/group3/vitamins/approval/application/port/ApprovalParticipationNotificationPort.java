package com.group3.vitamins.approval.application.port;

import java.util.Collection;
import java.util.List;

/** 참여 불가 사원이 영향을 주는 결재와 대행 처리 알림 수신자를 조회하는 아웃바운드 포트. */
public interface ApprovalParticipationNotificationPort {

    /** 진행 중 회차에서 해당 사원이 아직 처리하지 않은(ACTIVE/WAITING) 결재선. */
    List<Target> findPendingApproverTargets(String userId, Long companyId);

    /** 해당 사원이 현재 기안자 또는 대행 기안자인 진행 중·반려 결재. */
    List<Target> findDrafterTargets(String userId, Long companyId);

    /** 프로젝트 권한 상속과 스텝 오버라이드를 반영한 블록별 활성 스텝 EDITOR. */
    List<Editor> findActiveStepEditors(Collection<Long> blockIds, Long companyId);

    record Target(
            Long approvalId,
            Long revisionId,
            Long blockId,
            String title,
            String drafterId,
            String actingDrafterId
    ) {
    }

    record Editor(Long blockId, String userId) {
    }
}
