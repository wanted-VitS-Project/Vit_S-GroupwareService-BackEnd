package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.policy.ApprovalListScopePolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalViewPolicy;
import com.group3.vitamins.approval.application.port.ApprovalLineDetailPort;
import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.application.query.GetApprovalDetailQuery;
import com.group3.vitamins.approval.application.query.GetApprovalHistoryQuery;
import com.group3.vitamins.approval.application.query.GetApprovalRevisionQuery;
import com.group3.vitamins.approval.application.query.ListApprovalsQuery;
import com.group3.vitamins.approval.application.result.ApprovalDetailResult;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalHistoryResult;
import com.group3.vitamins.approval.application.result.ApprovalRevisionHistoryItem;
import com.group3.vitamins.approval.application.result.ApprovalLineDetailView;
import com.group3.vitamins.approval.application.result.ApprovalLinePreviewResult;
import com.group3.vitamins.approval.application.result.ApprovalListItemResult;
import com.group3.vitamins.approval.application.result.ApprovalListPageResult;
import com.group3.vitamins.approval.application.result.ApprovalRevisionDetail;
import com.group3.vitamins.approval.application.result.BlockOriginView;
import com.group3.vitamins.approval.application.usecase.ApprovalQueryUseCase;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalListMapper;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalLinePreviewRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalListRow;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 결재 회차 상세조회(MGT-005). 읽기 전용이라 쓰기용 {@code ApprovalCommandService} 와 분리한다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApprovalQueryService implements ApprovalQueryUseCase {

    private final ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    private final ApprovalViewPolicy viewPolicy;
    private final ApprovalListScopePolicy listScopePolicy;
    private final EmployeeCatalogPort employeeCatalogPort;
    private final FileCatalogPort fileCatalogPort;
    private final ApprovalRepository approvalRepository;
    private final ApprovalLineDetailPort approvalLineDetailPort;
    private final ApprovalListMapper approvalListMapper;
    private final BlockCatalogPort blockCatalogPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public ApprovalRevisionDetail getRevisionDetail(GetApprovalRevisionQuery query) {
        log.info("결재 회차 상세조회 요청 - approvalId={}, revisionId={}, requesterId={}",
                query.approvalId(), query.revisionId(), query.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(query.approvalId());
        ApprovalRevision revision = revisionEligibilityPolicy.getRevisionOrThrow(query.approvalId(), query.revisionId());
        List<ApprovalLine> lines = approvalRepository.findLinesByRevisionId(query.revisionId());

        viewPolicy.assertViewable(approval, lines, query.requesterId());

        EmployeeSummary drafter = employeeCatalogPort.findEmployee(approval.getDrafterId())
                .orElse(unavailableEmployee(approval.getDrafterId()));
        EmployeeSummary actingDrafter = approval.getActingDrafterId() == null ? null
                : employeeCatalogPort.findEmployee(approval.getActingDrafterId())
                        .orElse(unavailableEmployee(approval.getActingDrafterId()));

        List<ApprovalDocumentView> documents = loadDocumentViews(query.revisionId());

        // MyBatis 조인 조회(approval_line+employee+department+job_position)로 대체 — 결재선마다
        // employeeCatalogPort.findEmployee() 를 따로 부르던 N+1 을 없앤다(MYBATIS.md §1, INV-11 유지)
        List<ApprovalLineDetailView> lineViews = approvalLineDetailPort.findLineDetails(query.revisionId());

        return new ApprovalRevisionDetail(revision.getRevisionId(), revision.getRevisionNo(),
                revision.getTitle(), revision.getContent(),
                approval.getDrafterId(), drafter.name(), drafter.department(), drafter.position(),
                drafter.participationUnavailable(), approval.getActingDrafterId(),
                actingDrafter == null ? null : actingDrafter.name(),
                revision.getStatus().name(), revision.getSubmittedAt(), revision.getFinishedAt(),
                documents, lineViews);
    }

    @Override
    public ApprovalListPageResult listApprovals(ListApprovalsQuery query) {
        log.info("결재관리 목록조회 요청 - scope={}, requesterId={}, page={}, size={}",
                query.scope(), query.requesterId(), query.page(), query.size());

        // ⚠️ 요청 scope 가 아니라 해석된 scope 로 분기한다. ADMIN 은 기본값(drafted)이 all 로 올라가는데,
        //    query.scope() 로 분기하면 그 승격이 무시돼 본인 기안 필터가 걸리고 결과가 조용히 0건이 된다.
        String scope = listScopePolicy.resolveScope(query.scope(), query.requesterId());

        String drafterId = null;
        String approverId = null;
        String activeApproverId = null;

        switch (scope) {
            case "pending" -> activeApproverId = query.requesterId();
            case "all" -> {
                drafterId = query.drafterId();
                approverId = query.approverId();
            }
            default -> drafterId = query.requesterId(); // "drafted" (기본값)
        }

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        long totalElements = approvalListMapper.countApprovals(
                companyId, query.status(), drafterId, approverId, activeApproverId,
                query.fromDate(), query.toDate(), query.keyword(), query.revisionNo());

        List<ApprovalListRow> rows = approvalListMapper.findApprovals(
                companyId, query.status(), drafterId, approverId, activeApproverId,
                query.fromDate(), query.toDate(), query.keyword(), query.revisionNo(),
                query.page() * query.size(), query.size());

        List<Long> revisionIds = rows.stream().map(ApprovalListRow::currentRevisionId).distinct().toList();
        Map<Long, List<ApprovalLinePreviewResult>> linesByRevisionId = revisionIds.isEmpty()
                ? Map.of()
                : approvalListMapper.findLinePreviewsByRevisionIds(revisionIds).stream()
                        .collect(Collectors.groupingBy(ApprovalLinePreviewRow::revisionId,
                                Collectors.mapping(r -> new ApprovalLinePreviewResult(
                                        r.approverId(), r.approverName(), r.sequenceNo(), r.status()),
                                        Collectors.toList())));

        List<ApprovalListItemResult> content = rows.stream()
                .map(row -> new ApprovalListItemResult(
                        row.approvalId(), row.title(), row.status(), row.currentRevisionNo(),
                        row.drafterId(), row.drafterName(), row.currentApproverId(), row.currentApproverName(),
                        row.projectId(), row.projectName(), row.stepId(), row.stepName(),
                        linesByRevisionId.getOrDefault(row.currentRevisionId(), List.of()),
                        row.createdAt(), row.submittedAt(), row.completedAt()))
                .toList();

        int totalPages = query.size() == 0 ? 0 : (int) Math.ceil((double) totalElements / query.size());
        return new ApprovalListPageResult(content, totalElements, totalPages);
    }

    @Override
    public ApprovalDetailResult getApprovalDetail(GetApprovalDetailQuery query) {
        log.info("결재 상세조회 요청 - approvalId={}, requesterId={}", query.approvalId(), query.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(query.approvalId());

        // 전 회차 결재선을 통틀어 열람 자격을 판정한다 — 과거 회차 참여자도 결재 이력을 볼 수 있다.
        List<ApprovalLine> allLines = approvalRepository.findLinesByApprovalId(query.approvalId());
        viewPolicy.assertViewable(approval, allLines, query.requesterId());

        ApprovalRevision revision = approvalRepository.findLatestRevisionReadOnly(query.approvalId())
                .orElseThrow(() -> new NotFoundException(ApprovalErrorCode.APPROVAL_REVISION_NOT_FOUND));

        EmployeeSummary drafter = employeeCatalogPort.findEmployee(approval.getDrafterId())
                .orElse(unavailableEmployee(approval.getDrafterId()));
        EmployeeSummary actingDrafter = approval.getActingDrafterId() == null ? null
                : employeeCatalogPort.findEmployee(approval.getActingDrafterId())
                        .orElse(unavailableEmployee(approval.getActingDrafterId()));

        List<ApprovalDocumentView> documents = loadDocumentViews(revision.getRevisionId());

        List<ApprovalLineDetailView> lineViews = approvalLineDetailPort.findLineDetails(revision.getRevisionId());

        BlockSummary block = blockCatalogPort.findBlock(approval.getBlockId())
                .orElseThrow(() -> new NotFoundException(ApprovalErrorCode.APPROVAL_NOT_FOUND));
        BlockOriginView blockOrigin = new BlockOriginView(block.blockId(), block.stepId(), block.projectId());

        return new ApprovalDetailResult(revision.getRevisionId(), revision.getRevisionNo(),
                revision.getTitle(), revision.getContent(), approval.getDrafterId(), drafter.name(),
                drafter.department(), drafter.position(),
                drafter.participationUnavailable(), approval.getActingDrafterId(),
                actingDrafter == null ? null : actingDrafter.name(),
                revision.getStatus().name(), documents, lineViews, blockOrigin);
    }

    @Override
    public ApprovalHistoryResult getApprovalHistory(GetApprovalHistoryQuery query) {
        log.info("결재 이력조회 요청 - approvalId={}, requesterId={}", query.approvalId(), query.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(query.approvalId());
        List<ApprovalLine> allLines = approvalRepository.findLinesByApprovalId(query.approvalId());

        viewPolicy.assertViewable(approval, allLines, query.requesterId());

        List<ApprovalRevisionHistoryItem> content = approvalRepository.findRevisionsByApprovalId(query.approvalId())
                .stream()
                .map(r -> new ApprovalRevisionHistoryItem(r.getRevisionId(), r.getRevisionNo(), r.getStatus().name(),
                        r.getSubmittedAt(), r.getFinishedAt(), r.getRevisionNo() == approval.getCurrentRevisionNo()))
                .toList();

        return new ApprovalHistoryResult(content);
    }

    /**
     * 회차 첨부 목록 조립(MGT-005·MGT-006 공통).
     *
     * <p>첨부 수와 무관하게 <b>쿼리 2발</b>이다(문서 행 1 + 파일 배치 1). 예전에는 첨부마다
     * {@code fileCatalogPort.findFileVersion()} 을 불렀고 그 안에서 또 2쿼리가 나가, 첨부 5개면
     * 10쿼리였다.
     *
     * <p>⚠️ <b>{@code documents} 의 순서를 그대로 유지한다.</b> 배치 결과 맵으로 뒤집어 조립하면
     * 첨부 순서가 파일 버전 ID 순으로 바뀐다 — 화면에는 등록 순서로 보여야 하므로 원본 목록을
     * 기준으로 돌면서 맵에서 꺼낸다.
     */
    private List<ApprovalDocumentView> loadDocumentViews(Long revisionId) {
        List<ApprovalDocument> documents = approvalRepository.findDocumentsByRevisionId(revisionId);

        List<Long> fileVersionIds = documents.stream()
                .map(ApprovalDocument::getFileVersionId)
                .distinct()
                .toList();
        Map<Long, FileVersionSummary> filesByVersionId = fileCatalogPort.findFileVersions(fileVersionIds);

        return documents.stream()
                .map(doc -> {
                    // 버전 행을 못 찾으면 휴지통 여부를 판단할 근거가 없으니 false 로 둔다
                    FileVersionSummary file = filesByVersionId.getOrDefault(doc.getFileVersionId(),
                            new FileVersionSummary(doc.getFileVersionId(), null, null, null, null, false));
                    return new ApprovalDocumentView(
                            doc.getDocumentId(), doc.getFileVersionId(), file.fileName(), file.fileSize(),
                            file.uploadedAt(), file.fileDeleted());
                })
                .toList();
    }

    private EmployeeSummary unavailableEmployee(String userId) {
        return new EmployeeSummary(userId, null, null, null, null, null,
                null, null, null);
    }
}
