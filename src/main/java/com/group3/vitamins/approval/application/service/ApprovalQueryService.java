package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalViewPolicy;
import com.group3.vitamins.approval.application.port.ApprovalLineDetailPort;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.application.query.GetApprovalRevisionQuery;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalLineDetailView;
import com.group3.vitamins.approval.application.result.ApprovalRevisionDetail;
import com.group3.vitamins.approval.application.usecase.ApprovalQueryUseCase;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 결재 회차 상세조회(MGT-005). 읽기 전용이라 쓰기용 {@code ApprovalCommandService} 와 분리한다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApprovalQueryService implements ApprovalQueryUseCase {

    private final ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    private final ApprovalViewPolicy viewPolicy;
    private final EmployeeCatalogPort employeeCatalogPort;
    private final FileCatalogPort fileCatalogPort;
    private final ApprovalRepository approvalRepository;
    private final ApprovalLineDetailPort approvalLineDetailPort;

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
}
