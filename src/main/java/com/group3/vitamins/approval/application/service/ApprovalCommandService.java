package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.command.CreateApprovalCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalRevisionCommand;
import com.group3.vitamins.approval.application.policy.ApprovalBlockEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.result.ApprovalLineView;
import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import com.group3.vitamins.approval.application.usecase.ApprovalCommandUseCase;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import com.group3.vitamins.approval.domain.model.NewApprovalLine;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 결재 블록 생성(APR-001) · 제목·내용 수정(APR-002) · 결재선 등록·수정(APR-009~014) ·
 * 재상신 회차 생성(SUB-005~009). {@code block} 행 자체는 만들지 않는다 — 이미 존재하는 blockId 에
 * {@code approval}+1회차 {@code approval_revision} 만 붙인다(INV-08).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApprovalCommandService implements ApprovalCommandUseCase {

    private final ApprovalBlockEligibilityPolicy blockEligibilityPolicy;
    private final ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    private final ApprovalLineEligibilityPolicy lineEligibilityPolicy;
    private final EmployeeCatalogPort employeeCatalogPort;
    private final ApprovalRepository approvalRepository;

    @Override
    public ApprovalWithRevision createApproval(CreateApprovalCommand command) {
        log.info("결재 블록 생성 요청 - blockId={}, drafterId={}", command.blockId(), command.drafterId());

        BlockSummary block = blockEligibilityPolicy.getApprovalBlockOrThrow(command.blockId());
        blockEligibilityPolicy.assertProjectMember(block.projectId(), command.drafterId());

        ApprovalWithRevision created = approvalRepository.createDraft(command.blockId(), command.drafterId());

        log.info("결재 블록 생성 완료 - blockId={}, approvalId={}",
                command.blockId(), created.approval().getApprovalId());
        return created;
    }

    @Override
    public ApprovalRevision updateRevisionDraft(UpdateApprovalRevisionCommand command) {
        log.info("결재 제목·내용 수정 요청 - approvalId={}, revisionId={}, requesterId={}",
                command.approvalId(), command.revisionId(), command.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());
        ApprovalRevision current =
                revisionEligibilityPolicy.getDraftRevisionOrThrow(command.approvalId(), command.revisionId());

        String title = command.title() != null ? command.title() : current.getTitle();
        String content = command.content() != null ? command.content() : current.getContent();

        ApprovalRevision updated = approvalRepository.updateDraftContent(command.revisionId(), title, content);

        log.info("결재 제목·내용 수정 완료 - revisionId={}", updated.getRevisionId());
        return updated;
    }

    @Override
    public List<ApprovalLineView> updateLines(UpdateApprovalLinesCommand command) {
        log.info("결재선 등록·수정 요청 - approvalId={}, revisionId={}, requesterId={}, 결재자 수={}",
                command.approvalId(), command.revisionId(), command.requesterId(), command.lines().size());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());
        revisionEligibilityPolicy.getDraftRevisionOrThrow(command.approvalId(), command.revisionId());

        lineEligibilityPolicy.assertNotEmpty(command.lines());
        lineEligibilityPolicy.assertOrderValid(
                command.lines().stream().map(UpdateApprovalLinesCommand.LineInput::order).toList());

        List<String> approverIds = command.lines().stream()
                .map(UpdateApprovalLinesCommand.LineInput::approverId)
                .toList();
        List<EmployeeSummary> employees =
                lineEligibilityPolicy.assertApproversEligible(approval.getBlockId(), approverIds);

        List<NewApprovalLine> newLines = command.lines().stream()
                .map(input -> new NewApprovalLine(input.approverId(), input.order()))
                .toList();
        List<ApprovalLine> savedLines = approvalRepository.replaceLines(command.revisionId(), newLines);

        List<ApprovalLineView> result = zipLinesWithEmployees(savedLines, employees);

        log.info("결재선 등록·수정 완료 - revisionId={}, 등록된 결재선 수={}", command.revisionId(), result.size());
        return result;
    }

    @Override
    public ApprovalResubmissionResult resubmit(ResubmitApprovalCommand command) {
        log.info("재상신 회차 생성 요청 - approvalId={}, requesterId={}", command.approvalId(), command.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());

        if (approval.getStatus() != ApprovalStatus.REJECTED) {
            log.warn("재상신 회차 생성 - REJECTED 아님 approvalId={}, status={}", command.approvalId(), approval.getStatus());
            throw new ConflictException(ApprovalErrorCode.APPROVAL_NOT_REJECTED);
        }

        ApprovalRevision latestRevision = approvalRepository.findLatestRevision(command.approvalId())
                .orElseThrow(() -> new IllegalStateException("no revision for approval " + command.approvalId()));

        if (latestRevision.getStatus() == ApprovalStatus.DRAFT) {
            // SUB-008 — 이미 준비된 DRAFT 회차가 있으면 새로 안 만들고 그대로 반환(멱등)
            return buildResumedResult(latestRevision, latestRevision.getRevisionNo() - 1, false);
        }

        // approval.status == REJECTED 면 최신 회차도 REJECTED 다(동기화 불변식) — SUB-002/PRC-007
        List<ApprovalLine> previousLines = approvalRepository.findLinesByRevisionId(latestRevision.getRevisionId());
        ApprovalLine rejectorLine = previousLines.stream()
                .filter(line -> line.getStatus() == ApprovalLineStatus.REJECTED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "no rejector line in rejected revision " + latestRevision.getRevisionId()));

        List<ApprovalLine> resumedFrom = previousLines.stream()
                .filter(line -> line.getSequenceNo() >= rejectorLine.getSequenceNo())
                .sorted(Comparator.comparingInt(ApprovalLine::getSequenceNo))
                .toList();

        int nextRevisionNo = latestRevision.getRevisionNo() + 1;
        ApprovalRevision newRevision = approvalRepository.createRevisionDraft(
                command.approvalId(), nextRevisionNo, latestRevision.getTitle(), latestRevision.getContent());

        List<Long> fileVersionIds = approvalRepository.findDocumentsByRevisionId(latestRevision.getRevisionId())
                .stream()
                .map(ApprovalDocument::getFileVersionId)
                .toList();
        List<ApprovalDocument> copiedDocuments =
                approvalRepository.copyDocuments(newRevision.getRevisionId(), fileVersionIds);

        List<NewApprovalLine> resumedLineInputs = new ArrayList<>();
        for (int i = 0; i < resumedFrom.size(); i++) {
            resumedLineInputs.add(new NewApprovalLine(resumedFrom.get(i).getApproverId(), i + 1));
        }
        List<ApprovalLine> newLines = approvalRepository.replaceLines(newRevision.getRevisionId(), resumedLineInputs);
        List<EmployeeSummary> employees = resumedFrom.stream()
                .map(line -> employeeCatalogPort.findEmployee(line.getApproverId())
                        .orElse(new EmployeeSummary(line.getApproverId(), null, null, null, null)))
                .toList();

        log.info("재상신 회차 생성 완료 - approvalId={}, newRevisionId={}, revisionNo={}",
                command.approvalId(), newRevision.getRevisionId(), newRevision.getRevisionNo());

        return new ApprovalResubmissionResult(newRevision, copiedDocuments,
                zipLinesWithEmployees(newLines, employees), latestRevision.getRevisionNo(), true);
    }

    /** SUB-008 멱등 반환 경로 — 이미 만들어진 DRAFT 회차의 문서·결재선을 그대로 읽어 응답을 조립한다 */
    private ApprovalResubmissionResult buildResumedResult(ApprovalRevision draftRevision,
                                                           int copiedFromRevisionNo, boolean created) {
        List<ApprovalDocument> documents = approvalRepository.findDocumentsByRevisionId(draftRevision.getRevisionId());
        List<ApprovalLine> lines = approvalRepository.findLinesByRevisionId(draftRevision.getRevisionId());
        List<EmployeeSummary> employees = lines.stream()
                .map(line -> employeeCatalogPort.findEmployee(line.getApproverId())
                        .orElse(new EmployeeSummary(line.getApproverId(), null, null, null, null)))
                .toList();

        return new ApprovalResubmissionResult(draftRevision, documents,
                zipLinesWithEmployees(lines, employees), copiedFromRevisionNo, created);
    }

    private List<ApprovalLineView> zipLinesWithEmployees(List<ApprovalLine> lines, List<EmployeeSummary> employees) {
        List<ApprovalLineView> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ApprovalLine line = lines.get(i);
            EmployeeSummary employee = employees.get(i);
            result.add(new ApprovalLineView(line.getLineId(), line.getApproverId(), line.getSequenceNo(),
                    employee.name(), employee.position(), employee.department()));
        }
        return result;
    }
}
