package com.group3.vitamins.issue.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper
public interface IssueQueryMapper {

    Optional<IssueBlockStepRow> findBlockStep(
            @Param("blockId") Long blockId,
            @Param("companyId") Long companyId
    );

    Optional<IssueRow> findIssue(
            @Param("issueId") Long issueId,
            @Param("companyId") Long companyId
    );

    List<IssueRow> findIssues(
            @Param("stepId") Long stepId,
            @Param("blockId") Long blockId,
            @Param("companyId") Long companyId
    );

    List<IssueAssigneeRow> findAssignees(
            @Param("issueIds") Collection<Long> issueIds,
            @Param("companyId") Long companyId
    );

    List<IssueAssigneeCandidateRow> findAssigneeCandidates(
            @Param("userIds") Collection<String> userIds,
            @Param("companyId") Long companyId
    );

    List<IssueRelatedBlockRow> findRelatedBlocks(
            @Param("issueIds") Collection<Long> issueIds,
            @Param("companyId") Long companyId
    );

    List<IssueBlockLinkableRow> findLinkableBlocks(
            @Param("blockIds") Collection<Long> blockIds,
            @Param("companyId") Long companyId
    );

    List<IssueCalendarRow> findMyCalendarIssues(
            @Param("userId") String userId,
            @Param("companyId") Long companyId
    );

    Optional<Long> findProjectId(
            @Param("stepId") Long stepId,
            @Param("companyId") Long companyId
    );

    List<StepSummaryRow> findStepsByProject(
            @Param("projectId") Long projectId,
            @Param("companyId") Long companyId
    );

    List<IssueRow> findIssuesByProject(
            @Param("projectId") Long projectId,
            @Param("companyId") Long companyId
    );
}
