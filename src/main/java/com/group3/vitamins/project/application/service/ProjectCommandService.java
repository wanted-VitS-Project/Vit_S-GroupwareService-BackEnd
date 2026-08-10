package com.group3.vitamins.project.application.service;

import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.command.DeleteProjectCommand;
import com.group3.vitamins.project.application.command.LinkBusinessCategoriesCommand;
import com.group3.vitamins.project.application.command.UnlinkBusinessCategoryCommand;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.ProjectCategoryResult;
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
import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import com.group3.vitamins.project.application.result.ProjectUpdateResult;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
// ── import 추가
import com.group3.vitamins.project.application.command.ChangeProjectStatusCommand;
import com.group3.vitamins.project.application.command.CloseProjectCommand;
import com.group3.vitamins.project.application.result.ProjectCloseResult;
import com.group3.vitamins.project.application.result.ProjectStatusResult;
import com.group3.vitamins.project.domain.model.CloseReasonCode;
import com.group3.vitamins.project.domain.model.ProjectStatus;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService implements ProjectCommandUseCase {


    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    private final BusinessCategoryLookupPort businessCategoryLookupPort;
    private final EmployeeLookupPort employeeLookupPort;
    private final StepStatLookupPort stepStatLookupPort;
    private final ProjectAccessUseCase projectAccessUseCase;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public ProjectResult createProject(CreateProjectCommand command) {
        validateDateRange(command.startedOn(), command.endedOn());
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        if (command.bidNoticeId() != null) {
            checkBidNoticeNotLinked(command.bidNoticeId(), companyId);
        }

        List<Long> categoryIds = command.businessCategoryIds() == null
                ? List.of() : command.businessCategoryIds().stream().distinct().toList();
        List<BusinessCategorySummary> categories = resolveCategories(categoryIds);

        LocalDateTime now = LocalDateTime.now();
        Project saved = projectRepository.save(Project.create(
                command.bidNoticeId(), command.name(), command.description(), command.clientName(),
                command.startedOn(), command.endedOn(), command.contractAmount(),
                command.requesterUserId(), now, companyId));

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

    /** 수정 화면이 폼 전체를 보내므로 받은 값으로 덮어쓴다. null 은 해당 값을 비운다. */
    @Override
    public ProjectUpdateResult updateProject(UpdateProjectCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        validateDateRange(command.startedOn(), command.endedOn());

        Project updated = projectRepository.save(project.update(
                command.name(), command.description(), command.clientName(),
                command.startedOn(), command.endedOn(), command.contractAmount(),
                LocalDateTime.now()));

        return new ProjectUpdateResult(updated.getProjectId(), updated.getName(),
                updated.getClientName(), updated.getStartedOn(), updated.getEndedOn(),
                updated.getContractAmount(), updated.getUpdatedAt());
    }

    /** 상태를 바꾼다. 역방향도 허용하고 CLOSED 만 거부한다 (PRJ-003). */
    @Override
    public ProjectStatusResult changeStatus(ChangeProjectStatusCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        Project updated = projectRepository.save(
                project.changeStatus(parseStatus(command.status()), LocalDateTime.now()));

        return new ProjectStatusResult(updated.getProjectId(),
                updated.getStatus().name(), updated.getUpdatedAt());
    }

    /** 사유를 붙여 종결한다. 상태 제한이 없다 — 진행 중이든 정산 중이든 종결할 수 있다 (PRJ-004). */
    @Override
    public ProjectCloseResult closeProject(CloseProjectCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        Project closed = projectRepository.save(project.close(
                parseCloseReason(command.closeReasonCode()), command.closeReasonNote(),
                LocalDateTime.now()));

        return new ProjectCloseResult(closed.getProjectId(), closed.getStatus().name(),
                closed.getCloseReasonCode().name(), closed.getCloseReasonNote(),
                closed.getClosedAt());
    }

    /**
     * 상태 문자열을 판정한다. CLOSED 는 종결 API 소관이라 여기서 거부한다.
     *
     * <p>enum 소속 자체는 애노테이션으로도 막을 수 있지만, "값은 유효하나 이 API 에서만 금지" 는
     * 형식이 아니라 API 규칙이라 응용 계층에 남긴다.
     */
    private ProjectStatus parseStatus(String status) {
        if (status == null) {
            throw new ValidationException(ProjectErrorCode.PROJECT_STATUS_INVALID);
        }
        ProjectStatus parsed;
        try {
            parsed = ProjectStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ProjectErrorCode.PROJECT_STATUS_INVALID);
        }
        if (parsed == ProjectStatus.CLOSED) {
            throw new ValidationException(ProjectErrorCode.PROJECT_STATUS_INVALID);
        }
        return parsed;
    }

    /** 종결 사유 코드를 판정한다. 누락과 오타를 다른 코드로 구분한다 (PRJ-005). */
    private CloseReasonCode parseCloseReason(String closeReasonCode) {
        if (closeReasonCode == null || closeReasonCode.isBlank()) {
            throw new ValidationException(ProjectErrorCode.CLOSE_REASON_REQUIRED);
        }
        try {
            return CloseReasonCode.valueOf(closeReasonCode);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ProjectErrorCode.CLOSE_REASON_INVALID);
        }
    }

    /**
     * 시작일·종료일이 둘 다 있을 때만 순서를 검증한다. 둘 다 선택 입력이다.
     *
     * <p>필수·길이·음수 검증은 요청 DTO 의 Bean Validation 이 맡는다 — 여기 남은 건
     * 두 필드의 <b>관계</b>라 필드 단위 애노테이션으로 표현할 수 없는 규칙뿐이다.
     */
    private void validateDateRange(LocalDate startedOn, LocalDate endedOn) {
        if (startedOn != null && endedOn != null && startedOn.isAfter(endedOn)) {
            throw new ValidationException(ProjectErrorCode.PROJECT_DATE_RANGE_INVALID);
        }
    }

    /**
     * 사업 카테고리를 연결한다 (PRJ-007). 응답에는 <b>연결 후 전체</b> 카테고리를 담는다 —
     * 화면이 방금 추가분만 받아서 목록을 갱신하면 기존 연결이 사라진 것처럼 보인다.
     */
    @Override
    public ProjectCategoryResult linkBusinessCategories(LinkBusinessCategoriesCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        List<Long> categoryIds = requireCategoryIds(command.categoryIds());
        resolveCategories(categoryIds);

        List<Long> linked = projectBusinessCategoryRepository.findCategoryIds(command.projectId());
        if (categoryIds.stream().anyMatch(linked::contains)) {
            throw new ConflictException(ProjectErrorCode.BUSINESS_CATEGORY_DUPLICATED);
        }

        projectBusinessCategoryRepository.linkAll(command.projectId(), categoryIds);

        // ⚠️ 응답은 describeLinked 다. resolveCategories(검증용)로 돌리면 예전에 연결해 둔 카테고리가
        //    그 사이 삭제됐을 때 개수가 안 맞아 404 가 나고, 방금 성공한 연결까지 롤백된다.
        //    그 프로젝트는 카테고리를 영영 추가할 수 없게 된다.
        return new ProjectCategoryResult(command.projectId(), describeLinked(
                projectBusinessCategoryRepository.findCategoryIds(command.projectId())));
    }

    /** 연결을 끊는다. 조인 행이라 하드 삭제다 — soft 로 두면 UNIQUE 를 시체가 점유해 재연결이 막힌다. */
    @Override
    public void unlinkBusinessCategory(UnlinkBusinessCategoryCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        if (!projectBusinessCategoryRepository.unlink(command.projectId(), command.categoryId())) {
            throw new NotFoundException(ProjectErrorCode.BUSINESS_CATEGORY_NOT_LINKED);
        }
    }

    /**
     * 논리 삭제한다 (PRJ-014). <b>진행 전 + 스텝 0개</b> 일 때만 허용한다 —
     * 이미 굴러간 프로젝트는 삭제가 아니라 종결(close)로 남긴다.
     *
     * <p>블록 수는 따로 세지 않는다. {@code block.step_id} 가 NOT NULL 이라
     * <b>스텝이 0개면 블록도 0개</b>다 — 두 번 세면 쿼리만 늘고 판정은 같다.
     *
     * <p>⚠️ 하위 정리를 하지 않는다. 스텝이 없다는 것이 전제라 지울 대상 자체가 없다.
     */
    @Override
    public void deleteProject(DeleteProjectCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        if (project.getStatus() != ProjectStatus.NOT_STARTED
                || stepStatLookupPort.countByProjectId(command.projectId()).totalCount() > 0) {
            throw new ConflictException(ProjectErrorCode.PROJECT_DELETE_NOT_ALLOWED);
        }

        projectRepository.save(project.delete(LocalDateTime.now()));
    }

    /** 빈 목록은 거부한다. 중복은 제거해 같은 카테고리를 두 번 넣는 요청을 그대로 통과시킨다. */
    private List<Long> requireCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new ValidationException(ProjectErrorCode.CATEGORY_IDS_REQUIRED);
        }
        return categoryIds.stream().distinct().toList();
    }

    /** 현재 회사 소유의 프로젝트를 불러온다. 타사 프로젝트와 삭제분은 404 로 귀결된다. */
    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId, currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    /**
     * 같은 회사에서 같은 공고로 이미 프로젝트가 만들어졌으면 막는다 —
     * 안 막으면 UNIQUE 제약이 DB 500 으로 샌다. 다른 회사가 같은 공고를 쓰는 것은 정상이라 걸리지 않는다.
     */
    private void checkBidNoticeNotLinked(Long bidNoticeId, Long companyId) {
        projectRepository.findByBidNoticeId(bidNoticeId, companyId).ifPresent(existing -> {
            throw new ConflictException(ProjectErrorCode.PROJECT_BID_NOTICE_ALREADY_LINKED);
        });
    }

    /**
     * <b>요청으로 들어온</b> 카테고리 id 가 전부 살아있는지 확인하고 응답용 요약으로 바꾼다.
     *
     * <p>⛔ 이미 연결된 카테고리를 여기 태우지 마라 — 그중 하나가 삭제돼 있으면 개수가 안 맞아
     * 404 가 난다. 그건 {@link #describeLinked} 소관이다.
     */
    private List<BusinessCategorySummary> resolveCategories(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        List<BusinessCategoryLookupPort.BusinessCategoryView> found =
                businessCategoryLookupPort.findByIds(
                        categoryIds, currentCompanyIdProvider.currentCompanyId());
        if (found.size() != Set.copyOf(categoryIds).size()) {
            throw new NotFoundException(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NOT_FOUND);
        }
        return found.stream()
                // 쓰기 경로다 — findByIds 가 deleted_at IS NULL 을 보므로 삭제된 카테고리는 여기 못 온다.
                .map(view -> new BusinessCategorySummary(view.categoryId(), view.name(), view.code(), false))
                .toList();
    }

    /**
     * <b>이미 연결된</b> 카테고리를 응답용 요약으로 옮긴다. 존재 검증을 하지 않는다 —
     * 연결 시점에 이미 검증했고, 그 뒤 삭제됐다는 사실은 막을 게 아니라 알릴 값이다 (DELETE.md D-6).
     */
    private List<BusinessCategorySummary> describeLinked(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return businessCategoryLookupPort.findRefsByIds(
                        categoryIds, currentCompanyIdProvider.currentCompanyId()).stream()
                .map(ref -> new BusinessCategorySummary(
                        ref.categoryId(), ref.name(), ref.code(), ref.deleted()))
                .toList();
    }
}
