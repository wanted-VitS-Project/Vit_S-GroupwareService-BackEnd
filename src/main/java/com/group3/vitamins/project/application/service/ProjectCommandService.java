package com.group3.vitamins.project.application.service;

import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.usecase.ProjectCommandUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectMember;
import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService implements ProjectCommandUseCase {

    private static final int NAME_MAX_LENGTH = 300;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    private final BusinessCategoryLookupPort businessCategoryLookupPort;
    private final EmployeeLookupPort employeeLookupPort;

    @Override
    public ProjectResult createProject(CreateProjectCommand command) {
        validateName(command.name());
        validateDateRange(command.startedOn(), command.endedOn());
        if (command.bidNoticeId() != null) {
            checkBidNoticeNotLinked(command.bidNoticeId());
        }

        List<Long> categoryIds = command.businessCategoryIds() == null
                ? List.of() : command.businessCategoryIds().stream().distinct().toList();
        List<BusinessCategorySummary> categories = resolveCategories(categoryIds);

        LocalDateTime now = LocalDateTime.now();
        Project saved = projectRepository.save(Project.create(
                command.bidNoticeId(), command.name(), command.description(), command.clientName(),
                command.startedOn(), command.endedOn(), command.contractAmount(),
                command.requesterUserId(), now));

        projectMemberRepository.save(
                ProjectMember.createEditor(saved.getProjectId(), command.requesterUserId(), now));

        if (!categoryIds.isEmpty()) {
            projectBusinessCategoryRepository.linkAll(saved.getProjectId(), categoryIds);
        }

        String createdByName = employeeLookupPort.findNameByUserId(command.requesterUserId());

        return new ProjectResult(
                saved.getProjectId(), saved.getName(), saved.getClientName(),
                saved.getStatus().name(), saved.getStartedOn(), saved.getEndedOn(),
                saved.getContractAmount(), categories, saved.getBidNoticeId(),
                new ProjectResult.CreatedBy(command.requesterUserId(), createdByName),
                saved.getCreatedAt());
    }

    /** 과업명을 검증한다. null·공백·300자 초과를 막는다. */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(ProjectErrorCode.PROJECT_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new ValidationException(ProjectErrorCode.PROJECT_NAME_TOO_LONG);
        }
    }

    /** 시작일·종료일이 둘 다 있을 때만 순서를 검증한다. 둘 다 선택 입력이다. */
    private void validateDateRange(LocalDate startedOn, LocalDate endedOn) {
        if (startedOn != null && endedOn != null && startedOn.isAfter(endedOn)) {
            throw new ValidationException(ProjectErrorCode.PROJECT_DATE_RANGE_INVALID);
        }
    }

    /** 같은 공고로 이미 프로젝트가 만들어졌으면 막는다 — 안 막으면 UNIQUE 제약이 DB 500 으로 샌다. */
    private void checkBidNoticeNotLinked(Long bidNoticeId) {
        projectRepository.findByBidNoticeId(bidNoticeId).ifPresent(existing -> {
            throw new ConflictException(ProjectErrorCode.PROJECT_BID_NOTICE_ALREADY_LINKED);
        });
    }

    /** 요청된 카테고리 id 가 전부 존재하는지 확인하고 응답용 요약으로 바꾼다. */
    private List<BusinessCategorySummary> resolveCategories(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        List<BusinessCategoryLookupPort.BusinessCategoryView> found =
                businessCategoryLookupPort.findByIds(categoryIds);
        if (found.size() != Set.copyOf(categoryIds).size()) {
            throw new NotFoundException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NOT_FOUND);
        }
        return found.stream()
                .map(view -> new BusinessCategorySummary(view.categoryId(), view.name(), view.code()))
                .toList();
    }
}
