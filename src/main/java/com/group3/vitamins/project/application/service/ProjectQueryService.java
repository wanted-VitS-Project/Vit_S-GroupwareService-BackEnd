package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.query.ProjectDetailQuery;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.ProjectDetailResult;
import com.group3.vitamins.project.application.usecase.ProjectQueryUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService implements ProjectQueryUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    private final BusinessCategoryLookupPort businessCategoryLookupPort;
    private final StepStatLookupPort stepStatLookupPort;
    private final ProjectAccessPolicy projectAccessPolicy;

    @Override
    public ProjectDetailResult getProjectDetail(ProjectDetailQuery query) {
        Project project = projectRepository.findById(query.projectId())
                .orElseThrow(() -> new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));

        MemberPermission myPermission = projectAccessPolicy.resolvePermission(
                query.role(),
                projectMemberRepository.findPermission(query.projectId(), query.requesterUserId())
                        .orElse(null));

        StepStatLookupPort.StepStatView stat = stepStatLookupPort.countByProjectId(query.projectId());

        return new ProjectDetailResult(
                project.getProjectId(),
                project.getName(),
                project.getDescription(),
                project.getClientName(),
                project.getStatus().name(),
                project.getStartedOn(),
                project.getEndedOn(),
                project.getContractAmount(),
                progressRate(stat),
                stat.totalCount(),
                stat.doneCount(),
                resolveCategories(query.projectId()),
                project.getBidNoticeId(),
                project.getCloseReasonCode() == null ? null : project.getCloseReasonCode().name(),
                project.getCloseReasonNote(),
                myPermission.name(),
                project.getCreatedAt());
    }

    /** 완료 스텝 / 전체 스텝 비율. 스텝이 없으면 null 을 돌려 응답에서 빠지게 한다 (INV-04). */
    private Integer progressRate(StepStatLookupPort.StepStatView stat) {
        if (stat.totalCount() == 0) {
            return null;
        }
        return stat.doneCount() * 100 / stat.totalCount();
    }

    /** 연결된 카테고리를 이름·업무코드까지 채워 돌려준다. */
    private List<BusinessCategorySummary> resolveCategories(Long projectId) {
        List<Long> categoryIds = projectBusinessCategoryRepository.findCategoryIds(projectId);
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return businessCategoryLookupPort.findByIds(categoryIds).stream()
                .map(view -> new BusinessCategorySummary(view.categoryId(), view.name(), view.code()))
                .toList();
    }
}