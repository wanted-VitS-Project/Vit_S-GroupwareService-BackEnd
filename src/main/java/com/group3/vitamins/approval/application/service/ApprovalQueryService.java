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
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalListMapper;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalLinePreviewRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalListRow;
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

    @Override
    public ApprovalRevisionDetail getRevisionDetail(GetApprovalRevisionQuery query) {
        log.info("결재 회차 상세조회 요청 - approvalId={}, revisionId={}, requesterId={}",
                query.approvalId(), query.revisionId(), query.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(query.approvalId());
        ApprovalRevision revision = revisionEligibilityPolicy.getRevisionOrThrow(query.approvalId(), query.revisionId());
        List<ApprovalLine> lines = approvalRepository.findLinesByRevisionId(query.revisionId());

        viewPolicy.assertViewable(approval, lines, query.requesterId());

        EmployeeSummary drafter = employeeCatalogPort.findEmployee(approval.getDrafterId())
                .orElse(new EmployeeSummary(approval.getDrafterId(), null, null, null, null));

        List<ApprovalDocumentView> documents = approvalRepository.findDocumentsByRevisionId(query.revisionId())
                .stream()
                .map(doc -> {
                    FileVersionSummary file = fileCatalogPort.findFileVersion(doc.getFileVersionId())
                            .orElse(new FileVersionSummary(doc.getFileVersionId(), null, null, null, null));
                    return new ApprovalDocumentView(
                            doc.getDocumentId(), doc.getFileVersionId(), file.fileName(), file.fileSize(), file.uploadedAt());
                })
                .toList();

        // MyBatis 조인 조회(approval_line+employee+department+job_position)로 대체 — 결재선마다
        // employeeCatalogPort.findEmployee() 를 따로 부르던 N+1 을 없앤다(MYBATIS.md §1, INV-11 유지)
        List<ApprovalLineDetailView> lineViews = approvalLineDetailPort.findLineDetails(query.revisionId());

        return new ApprovalRevisionDetail(revision.getRevisionId(), revision.getRevisionNo(),
                revision.getTitle(), revision.getContent(),
                approval.getDrafterId(), drafter.name(), drafter.department(), drafter.position(),
                revision.getStatus().name(), revision.getSubmittedAt(), revision.getFinishedAt(),
                documents, lineViews);
    }

    @Override
    public ApprovalListPageResult listApprovals(ListApprovalsQuery query) {
        log.info("결재관리 목록조회 요청 - scope={}, requesterId={}, page={}, size={}",
                query.scope(), query.requesterId(), query.page(), query.size());

        String drafterId = null;
        String approverId = null;
        String activeApproverId = null;

        switch (query.scope()) {
            case "pending" -> activeApproverId = query.requesterId();
            case "all" -> {
                listScopePolicy.assertScopeAllAllowed(query.requesterId());
                drafterId = query.drafterId();
                approverId = query.approverId();
            }
            default -> drafterId = query.requesterId(); // "drafted" (기본값)
        }

        long totalElements = approvalListMapper.countApprovals(
                query.status(), drafterId, approverId, activeApproverId,
                query.fromDate(), query.toDate(), query.keyword(), query.revisionNo());

        List<ApprovalListRow> rows = approvalListMapper.findApprovals(
                query.status(), drafterId, approverId, activeApproverId,
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
        ApprovalRevision revision = approvalRepository.findLatestRevisionReadOnly(query.approvalId())
                .orElseThrow(() -> new NotFoundException(ApprovalErrorCode.APPROVAL_REVISION_NOT_FOUND));
        List<ApprovalLine> lines = approvalRepository.findLinesByRevisionId(revision.getRevisionId());

        viewPolicy.assertViewable(approval, lines, query.requesterId());

        EmployeeSummary drafter = employeeCatalogPort.findEmployee(approval.getDrafterId())
                .orElse(new EmployeeSummary(approval.getDrafterId(), null, null, null, null));

        List<ApprovalDocumentView> documents = approvalRepository.findDocumentsByRevisionId(revision.getRevisionId())
                .stream()
                .map(doc -> {
                    FileVersionSummary file = fileCatalogPort.findFileVersion(doc.getFileVersionId())
                            .orElse(new FileVersionSummary(doc.getFileVersionId(), null, null, null, null));
                    return new ApprovalDocumentView(
                            doc.getDocumentId(), doc.getFileVersionId(), file.fileName(), file.fileSize(), file.uploadedAt());
                })
                .toList();

        List<ApprovalLineDetailView> lineViews = approvalLineDetailPort.findLineDetails(revision.getRevisionId());

        BlockSummary block = blockCatalogPort.findBlock(approval.getBlockId())
                .orElseThrow(() -> new NotFoundException(ApprovalErrorCode.APPROVAL_NOT_FOUND));
        BlockOriginView blockOrigin = new BlockOriginView(block.blockId(), block.stepId(), block.projectId());

        return new ApprovalDetailResult(revision.getRevisionId(), revision.getRevisionNo(),
                revision.getTitle(), revision.getContent(), approval.getDrafterId(), drafter.name(),
                drafter.department(), drafter.position(),
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
}
