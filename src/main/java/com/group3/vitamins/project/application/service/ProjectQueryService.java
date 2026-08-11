package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.application.port.ProjectDetailQueryPort;
import com.group3.vitamins.project.application.port.ProjectListQueryPort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.query.ProjectDetailQuery;
import com.group3.vitamins.project.application.query.ProjectListCriteria;
import com.group3.vitamins.project.application.query.ProjectListQuery;
import com.group3.vitamins.project.application.query.ProjectProgressQuery;
import com.group3.vitamins.project.application.result.*;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.application.usecase.ProjectQueryUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService implements ProjectQueryUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final StepStatLookupPort stepStatLookupPort;
    private final ProjectListQueryPort projectListQueryPort;
    private final ProjectDetailQueryPort projectDetailQueryPort;
    private final ProjectAccessPolicy projectAccessPolicy;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public ProjectPageResult getProjects(ProjectListQuery query) {
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);

        ProjectListCriteria criteria = new ProjectListCriteria(
                currentCompanyIdProvider.currentCompanyId(),
                query.requesterUserId(),
                projectAccessPolicy.isGlobalAdmin(query.role()),
                normalizeStatus(query.status()),
                query.businessCategoryId(),
                query.startedOnFrom(),
                query.startedOnTo(),
                normalizeKeyword(query.keyword()),
                page * size,
                size);

        long totalElements = projectListQueryPort.count(criteria);
        List<ProjectSummary> content = totalElements == 0
                ? List.of()
                : projectListQueryPort.findPage(criteria).stream().map(this::toSummary).toList();

        return new ProjectPageResult(content, page, size, totalElements);
    }

    @Override
    public ProjectProgressResult getProjectProgress(ProjectProgressQuery query) {
        projectAccessUseCase.requireAccess(
                query.projectId(), query.requesterUserId(), query.role());

        StepStatLookupPort.StepStatView stat =
                stepStatLookupPort.countByProjectId(query.projectId());

        return new ProjectProgressResult(
                query.projectId(), stat.totalCount(), stat.doneCount(),
                progressRate(stat.totalCount(), stat.doneCount()));
    }

    @Override
    public ProjectDetailResult getProjectDetail(ProjectDetailQuery query) {
        ProjectDetailQueryPort.ProjectDetailView view =
                projectDetailQueryPort.findDetail(query.projectId(), query.requesterUserId(),
                                currentCompanyIdProvider.currentCompanyId())
                        .orElseThrow(() -> new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        MemberPermission myPermission =
                projectAccessPolicy.resolvePermission(query.role(), view.memberPermission());

        return new ProjectDetailResult(
                view.projectId(),
                view.name(),
                view.description(),
                view.clientName(),
                view.status(),
                view.startedOn(),
                view.endedOn(),
                view.contractAmount(),
                progressRate(view.stepCount(), view.doneStepCount()),
                view.stepCount(),
                view.doneStepCount(),
                view.businessCategories(),
                view.bidNoticeId(),
                view.closeReasonCode(),
                view.closeReasonNote(),
                myPermission.name(),
                view.createdAt(),
                view.version());
    }

    /** 허용되지 않은 상태 값이면 400 을 던진다. 빈 값은 필터 미적용으로 본다. */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ProjectStatus.valueOf(status).name();
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ProjectErrorCode.PROJECT_STATUS_INVALID, e);
        }
    }

    /** 빈 검색어는 필터 미적용으로 본다. */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    /** 조회 행을 카드 응답으로 옮긴다. 카테고리·참여자·집계는 SQL 에서 이미 채워져 있다. */
    private ProjectSummary toSummary(ProjectListQueryPort.ProjectListView view) {
        return new ProjectSummary(
                view.projectId(), view.name(), view.clientName(), view.status(),
                view.startedOn(), view.endedOn(), view.contractAmount(),
                progressRate(view.totalStepCount(), view.doneStepCount()),
                view.businessCategories(), view.members(),
                view.myIssueInProgressCount(), view.myApprovalInProgressCount(),
                view.version());
    }

    /** 완료 스텝 / 전체 스텝 비율. 스텝이 없으면 null 을 돌려 응답에서 빠지게 한다 (INV-04). */
    private Integer progressRate(int totalCount, int doneCount) {
        if (totalCount == 0) {
            return null;
        }
        return doneCount * 100 / totalCount;
    }
}