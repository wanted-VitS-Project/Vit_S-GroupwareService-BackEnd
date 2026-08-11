package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalParticipationNotificationRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalStepEditorRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Collection;

/** 사원 참여 불가 전환 시 영향을 받는 결재와 유효 스텝 EDITOR를 조회한다. */
@Mapper
public interface ApprovalParticipationNotificationMapper {

    List<ApprovalParticipationNotificationRow> findPendingApproverTargets(
            @Param("userId") String userId,
            @Param("companyId") Long companyId);

    List<ApprovalParticipationNotificationRow> findDrafterTargets(
            @Param("userId") String userId,
            @Param("companyId") Long companyId);

    List<ApprovalStepEditorRow> findActiveStepEditors(
            @Param("blockIds") Collection<Long> blockIds,
            @Param("companyId") Long companyId);
}
