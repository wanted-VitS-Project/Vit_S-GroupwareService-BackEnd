package com.group3.vitamins.project.application.service;

import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.command.DeleteProjectCommand;
import com.group3.vitamins.project.application.command.DuplicateProjectCommand;
import com.group3.vitamins.project.application.command.LinkBusinessCategoriesCommand;
import com.group3.vitamins.project.application.command.UnlinkBusinessCategoryCommand;
import com.group3.vitamins.project.application.port.BlockClonePort;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.port.StageCascadePort;
import com.group3.vitamins.project.application.port.StageClonePort;
import com.group3.vitamins.project.application.port.StepCascadePort;
import com.group3.vitamins.project.application.port.StepClonePort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.ProjectCategoryResult;
import com.group3.vitamins.project.application.result.ProjectDuplicateResult;
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
import java.util.Map;
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

    /** 복제 한 번에 허용하는 블록 수. 통짜 트랜잭션의 상한이다 (PRJ-018). */
    private static final int MAX_DUPLICATE_BLOCKS = 300;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    private final BusinessCategoryLookupPort businessCategoryLookupPort;
    private final EmployeeLookupPort employeeLookupPort;
    private final StepStatLookupPort stepStatLookupPort;
    private final StepCascadePort stepCascadePort;
    private final StageCascadePort stageCascadePort;
    private final StageClonePort stageClonePort;
    private final StepClonePort stepClonePort;
    private final BlockClonePort blockClonePort;
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
            projectBusinessCategoryRepository.linkAll(companyId, saved.getProjectId(), categoryIds);
        }

        String createdByName = employeeLookupPort.findNameByUserId(command.requesterUserId());

        return new ProjectResult(
                saved.getProjectId(), saved.getName(), saved.getClientName(),
                saved.getStatus().name(), saved.getStartedOn(), saved.getEndedOn(),
                saved.getContractAmount(), categories, saved.getBidNoticeId(),
                new ProjectResult.CreatedBy(command.requesterUserId(), createdByName),
                saved.getCreatedAt());
    }

    /**
     * 원본의 골격만 복제해 새 프로젝트를 만든다 (PRJ-018).
     *
     * <p>원본에는 <b>참여자 자격만</b> 요구한다 — 복제는 원본을 한 글자도 바꾸지 않으므로
     * 편집 권한을 요구할 근거가 없다. 복제되는 스텝명·블록 제목은 VIEWER 가 원본에서 이미 보는 값이다.
     *
     * <p>⚠️ <b>상한 검사를 복사 시작 전에</b> 한다. 블록 1개당 쿼리 3번(block INSERT → 상세 INSERT →
     * type_id UPDATE)이 한 트랜잭션에서 도는데, 다 만든 뒤에 거절하면 그만큼을 헛일하고 롤백한다.
     *
     * <p>⛔ 활동 로그를 남기지 않는다. 프로젝트 생성도 남기지 않고, {@code ActivityOccurredEvent} 가
     * {@code blockId} 를 필수로 요구해 <b>프로젝트 단위 로그를 낼 수단이 아직 없다</b>.
     * 프로젝트 계층 로그가 생길 때 생성과 함께 붙인다.
     */
    @Override
    public ProjectDuplicateResult duplicateProject(DuplicateProjectCommand command) {

        //원본을 볼 수 있는 사람인가 — 404(없음)가 403(권한)보다 먼저다
        projectAccessUseCase.requireAccess(
                command.sourceProjectId(), command.requesterUserId(), command.role());

        checkDuplicateSize(command.sourceProjectId());

        //복제본은 생성 경로를 그대로 탄다 — 이름 길이·날짜 범위·카테고리 존재·공고 중복 검증이 여기 있다
        ProjectResult created = createProject(command.toCreateCommand());

        // ⚠️ 순서가 곧 의존이다. 스텝은 새 stageId 를, 블록은 새 stepId 를 알아야 소속을 정한다.
        Map<Long, Long> stageIdMap =
                stageClonePort.cloneStages(command.sourceProjectId(), created.projectId());

        Map<Long, Long> stepIdMap = stepClonePort.cloneSteps(
                command.sourceProjectId(), created.projectId(), stageIdMap);

        BlockClonePort.ClonedBlocks blocks =
                blockClonePort.cloneBlocks(stepIdMap, command.requesterUserId());

        return new ProjectDuplicateResult(
                created,
                command.sourceProjectId(),
                new ProjectDuplicateResult.Copied(stageIdMap.size(), stepIdMap.size(), blocks.copied()),
                new ProjectDuplicateResult.Skipped(blocks.skipped()));
    }

    /**
     * 통짜 트랜잭션이라 원본이 크면 그만큼 오래 잡는다. 사람이 손으로 만든 프로젝트가 이 선을
     * 넘는 일은 사실상 없으므로, 넘었다면 복제가 아니라 원본을 손봐야 한다는 신호다.
     *
     * <p>메시지에 실제 개수를 담아 덮어쓴다 — 코드가 바뀌면 프론트 분기가 깨지므로 <b>메시지만</b> 바꾼다.
     */
    private void checkDuplicateSize(Long sourceProjectId) {
        int blocks = blockClonePort.countBlocks(sourceProjectId);
        if (blocks <= MAX_DUPLICATE_BLOCKS) {
            return;
        }
        throw new ValidationException(ProjectErrorCode.PROJECT_DUPLICATE_TOO_LARGE,
                "블록이 %d개라 복제할 수 없습니다. 한 번에 %d개까지 복제할 수 있습니다."
                        .formatted(blocks, MAX_DUPLICATE_BLOCKS));
    }

    /**
     * 수정 화면이 폼 전체를 보내므로 받은 값으로 덮어쓴다. null 은 해당 값을 비운다.
     *
     * <p><b>내가 조회한 버전이 아직 최신일 때만</b> 저장된다 (`.ai/docs/global/CONCURRENCY.md`).
     * ⚠️ {@code save()} 로 되돌리지 마라 — 검사와 저장이 한 문장이 아니면 그 틈에 남의 저장이 끼어들어
     * <b>예외도 로그도 없이</b> 갱신이 유실된다 (§6-4).
     */
    @Override
    public ProjectUpdateResult updateProject(UpdateProjectCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        validateDateRange(command.startedOn(), command.endedOn());

        int expected = command.overwrite() ? project.getVersion() : command.version();
        LocalDateTime now = LocalDateTime.now();

        int updated = projectRepository.updateIfVersionMatches(
                command.projectId(), project.getCompanyId(),
                command.name(), command.description(), command.clientName(),
                command.startedOn(), command.endedOn(), command.contractAmount(),
                now, expected);

        if (updated == 0) {
            throw new ConflictException(ProjectErrorCode.PROJECT_VERSION_CONFLICT);
        }

        return new ProjectUpdateResult(project.getProjectId(), command.name(),
                command.clientName(), command.startedOn(), command.endedOn(),
                command.contractAmount(), now, expected + 1);
    }

    /**
     * 상태를 바꾼다. 역방향도 허용하고 CLOSED 만 거부한다 (PRJ-003).
     *
     * <p>⚠️ <b>종결 정보를 도메인에게 먼저 계산시키고 그 결과를 UPDATE 에 넘긴다.</b>
     * CLOSED 에서 벗어나면 종결 사유·일시를 지우는 규칙이 {@code changeStatus} 안에 있는데,
     * SQL 에 같은 조건을 다시 쓰면 규칙이 두 곳으로 갈라져 한쪽만 고치는 사고가 난다.
     */
    @Override
    public ProjectStatusResult changeStatus(ChangeProjectStatusCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        int expected = command.overwrite() ? project.getVersion() : command.version();
        LocalDateTime now = LocalDateTime.now();
        Project changed = project.changeStatus(parseStatus(command.status()), now);

        int updated = projectRepository.changeStatusIfVersionMatches(
                command.projectId(), project.getCompanyId(), changed.getStatus(),
                changed.getCloseReasonCode(), changed.getCloseReasonNote(), changed.getClosedAt(),
                now, expected);

        if (updated == 0) {
            throw new ConflictException(ProjectErrorCode.PROJECT_VERSION_CONFLICT);
        }

        return new ProjectStatusResult(changed.getProjectId(),
                changed.getStatus().name(), now, expected + 1);
    }

    /**
     * 사유를 붙여 종결한다. 상태 제한이 없다 — 진행 중이든 정산 중이든 종결할 수 있다 (PRJ-004).
     *
     * <p>⛔ 여기에는 낙관적 락을 걸지 않는다 ({@code CONCURRENCY.md} §9). 종결은 사유가 필수라
     * 두 번 눌러도 같은 결과다 — 갱신 유실로 잃을 편집 내용이 없다.
     */
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

        projectBusinessCategoryRepository.linkAll(
                currentCompanyIdProvider.currentCompanyId(), command.projectId(), categoryIds);

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
     * 논리 삭제한다 (PRJ-014). <b>스텝이 남아 있어도 지운다</b> — 하위 스텝·블록·이슈까지 전파한다.
     *
     * <p>진행 전 + 스텝 0개면 그대로 지우고, 그 밖에는 {@link #requireDeleteConfirmed} 가 한 번
     * 되묻는다. ⚠️ <b>되묻기는 금지가 아니다</b> — {@code confirm=true} 재요청이면 통과한다.
     * 종결(close)은 「기록으로 남기는 마무리」로 남고, 삭제는 「없애기」다.
     *
     * <p><b>정리 대상은 계층 전파 + 스텝과 무관한 것</b> 두 갈래다. 후자는 스텝이 0개여도 남는다 —
     * 참여자(생성자 1건은 항상 있다) · 사업분류 연결 · 스테이지 · 스테이지 권한 기본값.
     * 프로젝트는 복구가 없으므로(§6-5) 전부 죽은 행이다. 연결 3종의 하드 삭제는
     * <b>D-3 예외</b>다 ({@code .ai/docs/global/DELETE.md} §2-2).
     * ⛔ {@code activity_log} 는 지우지 않는다 (D-5).
     *
     * <p>⚠️ <b>재무·결재 연결은 아직 안 끊는다.</b> {@code cash_flow.settle_block_id} ·
     * {@code tax_invoice.settle_block_id} 가 삭제된 정산 블록을 계속 가리키고, 진행 중 결재도
     * 그대로 남는다 (BLK-013 미구현 · STATE.md 백로그). 스텝 단건 삭제와 같은 상태다.
     */
    @Override
    public void deleteProject(DeleteProjectCommand command) {
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        Project project = requireProject(command.projectId());

        requireDeleteConfirmed(project,
                stepStatLookupPort.countByProjectId(command.projectId()).totalCount(),
                command.confirm());

        // ⚠️ 프로젝트 save() 를 맨 뒤에 둔다. 스텝·스테이지 정리가 @Modifying(clearAutomatically = true)
        //    벌크 UPDATE 라, 먼저 save() 하면 그 pending UPDATE 를 flush 하지 않고 영속성 컨텍스트를
        //    비운다. 커밋해도 프로젝트는 안 지워지고 하위만 지워진다 (DELETE.md §3-1).
        stepCascadePort.deleteByProjectId(command.projectId(), command.requesterUserId());
        stageCascadePort.deleteByProjectId(command.projectId());
        projectMemberRepository.deleteByProjectId(command.projectId());
        projectBusinessCategoryRepository.deleteByProjectId(command.projectId());

        projectRepository.save(project.delete(LocalDateTime.now()));
    }

    /**
     * 지울 게 남은 프로젝트면 한 번 되묻는다 (DEL-016 패턴). 진행 전 + 스텝 0개면 확인 없이 통과한다 —
     * 빈 껍데기라 사용자가 잃을 게 없다.
     *
     * <p>블록·이슈 수는 세지 않는다. {@code block.step_id}·{@code issue.step_id} 가 NOT NULL 이라
     * 스텝 수가 곧 규모의 척도이고, 두 번 더 세면 <b>확인만 하고 취소할 요청에도</b> 쿼리가 붙는다.
     */
    private void requireDeleteConfirmed(Project project, int stepCount, boolean confirm) {
        if (confirm || (project.getStatus() == ProjectStatus.NOT_STARTED && stepCount == 0)) {
            return;
        }
        throw new ConflictException(ProjectErrorCode.PROJECT_DELETE_CONFIRM_REQUIRED,
                "스텝 %d개와 그 안의 블록·이슈가 함께 삭제되며 되돌릴 수 없습니다. 삭제하려면 확인이 필요합니다."
                        .formatted(stepCount));
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
