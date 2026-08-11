package com.group3.vitamins.issue.infrastructure.adapter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper
public interface IssueQueryMapper {

    Optional<IssueBlockStepRow> findBlockStep(@Param("blockId") Long blockId);

    Optional<IssueRow> findIssue(@Param("issueId") Long issueId);

    List<IssueRow> findIssues(@Param("stepId") Long stepId, @Param("blockId") Long blockId);

    List<IssueAssigneeRow> findAssignees(@Param("issueIds") Collection<Long> issueIds);

    List<IssueRelatedBlockRow> findRelatedBlocks(@Param("issueIds") Collection<Long> issueIds);

    List<IssueCalendarRow> findMyCalendarIssues(@Param("userId") String userId);

    Optional<Long> findProjectId(@Param("stepId") Long stepId);

    List<StepSummaryRow> findStepsByProject(@Param("projectId") Long projectId);

    List<IssueRow> findIssuesByProject(@Param("projectId") Long projectId);
}
