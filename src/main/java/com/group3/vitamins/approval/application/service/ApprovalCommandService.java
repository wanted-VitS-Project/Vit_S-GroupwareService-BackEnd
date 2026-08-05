package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.approval.application.command.AddApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.RemoveApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.SubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalRevisionCommand;
import com.group3.vitamins.approval.application.policy.ApprovalDocumentEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalLineView;
import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import com.group3.vitamins.approval.application.result.ApprovalSubmissionResult;
import com.group3.vitamins.approval.application.usecase.ApprovalCommandUseCase;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.model.NewApprovalLine;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 제목·내용 수정(APR-002) · 결재선 등록·수정(APR-009~014) · 재상신 회차 생성(SUB-005~009) ·
 * 문서 추가·제거(APR-005~007) · 상신(SUB-001~004).
 *
 * <p>결재 상세 생성(APR-001)은 여기 없다 — {@code block} 생성과 같은 트랜잭션에서
 * {@code ApprovalBlockDetailAdapter}(블록팀의 {@code BlockDetailPort} 구현체)를 통해 이뤄진다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApprovalCommandService implements ApprovalCommandUseCase {

    private final ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    private final ApprovalLineEligibilityPolicy lineEligibilityPolicy;
    private final ApprovalDocumentEligibilityPolicy documentEligibilityPolicy;
    private final EmployeeCatalogPort employeeCatalogPort;
    private final FileCatalogPort fileCatalogPort;
    private final ApprovalRepository approvalRepository;
    private final DomainEventPublisher domainEventPublisher;

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

        List<ActivityFieldChange> changes = new ArrayList<>();
        if (!Objects.equals(current.getTitle(), updated.getTitle())) {
            changes.add(new ActivityFieldChange("title", current.getTitle(), updated.getTitle()));
        }
        if (!Objects.equals(current.getContent(), updated.getContent())) {
            changes.add(new ActivityFieldChange("content", current.getContent(), updated.getContent()));
        }
        publishActivity(ActivityLogAction.MODIFY, approval.getBlockId(), updated.getRevisionId(),
                resourceName(updated.getTitle()), command.requesterId(), changes);

        log.info("결재 제목·내용 수정 완료 - revisionId={}", updated.getRevisionId());
        return updated;
    }

    @Override
    public List<ApprovalLineView> updateLines(UpdateApprovalLinesCommand command) {
        log.info("결재선 등록·수정 요청 - approvalId={}, revisionId={}, requesterId={}, 결재자 수={}",
                command.approvalId(), command.revisionId(), command.requesterId(), command.lines().size());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());
        // 잠금 조회 — 상신(#7)이 이 트랜잭션 커밋 전까지 같은 회차의 상태를 못 바꾸게 막는다(CodeRabbit 지적 반영)
        ApprovalRevision revision =
                revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(command.approvalId(), command.revisionId());

        lineEligibilityPolicy.assertNotEmpty(command.lines());
        lineEligibilityPolicy.assertOrderValid(
                command.lines().stream().map(UpdateApprovalLinesCommand.LineInput::order).toList());

        List<String> approverIds = command.lines().stream()
                .map(UpdateApprovalLinesCommand.LineInput::approverId)
                .toList();
        List<EmployeeSummary> employees =
                lineEligibilityPolicy.assertApproversEligible(approval.getBlockId(), approverIds);

        List<ApprovalLine> previousLines = approvalRepository.findLinesByRevisionId(command.revisionId());
        String previousLineLabel = previousLines.stream()
                .sorted(Comparator.comparingInt(ApprovalLine::getSequenceNo))
                .map(ApprovalLine::getApproverId)
                .collect(Collectors.joining(","));

        List<NewApprovalLine> newLines = command.lines().stream()
                .map(input -> new NewApprovalLine(input.approverId(), input.order()))
                .toList();
        List<ApprovalLine> savedLines = approvalRepository.replaceLines(command.revisionId(), newLines);

        String newLineLabel = command.lines().stream()
                .sorted(Comparator.comparingInt(UpdateApprovalLinesCommand.LineInput::order))
                .map(UpdateApprovalLinesCommand.LineInput::approverId)
                .collect(Collectors.joining(","));
        if (!previousLineLabel.equals(newLineLabel)) {
            publishActivity(ActivityLogAction.MODIFY, approval.getBlockId(), command.revisionId(),
                    resourceName(revision.getTitle()), command.requesterId(),
                    List.of(new ActivityFieldChange("lines", previousLineLabel, newLineLabel)));
        }

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

        publishActivity(ActivityLogAction.CREATE, approval.getBlockId(), newRevision.getRevisionId(),
                resourceName(newRevision.getTitle()), command.requesterId(),
                List.of(new ActivityFieldChange(null, null, null)));

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

    @Override
    public ApprovalDocumentView addDocument(AddApprovalDocumentCommand command) {
        log.info("결재 문서 추가 요청 - approvalId={}, revisionId={}, fileVersionId={}",
                command.approvalId(), command.revisionId(), command.fileVersionId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());
        // 잠금 조회 — 상신과의 레이스 방지(#91과 동일한 이유)
        revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(command.approvalId(), command.revisionId());

        FileVersionSummary file = documentEligibilityPolicy.getReadyFileVersionOrThrow(command.fileVersionId());
        documentEligibilityPolicy.assertNotAlreadyLinked(command.revisionId(), command.fileVersionId());

        ApprovalDocument saved = approvalRepository.addDocument(command.revisionId(), command.fileVersionId());

        String documentLabel = file.fileName() != null ? file.fileName() : String.valueOf(file.fileVersionId());
        publishActivity(ActivityLogAction.CREATE, approval.getBlockId(), saved.getDocumentId(),
                resourceName(documentLabel), command.requesterId(),
                List.of(new ActivityFieldChange(null, null, null)));

        log.info("결재 문서 추가 완료 - documentId={}", saved.getDocumentId());
        return new ApprovalDocumentView(
                saved.getDocumentId(), file.fileVersionId(), file.fileName(), file.fileSize(), file.uploadedAt());
    }

    @Override
    public void removeDocument(RemoveApprovalDocumentCommand command) {
        log.info("결재 문서 제거 요청 - approvalId={}, revisionId={}, documentId={}",
                command.approvalId(), command.revisionId(), command.documentId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());
        revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(command.approvalId(), command.revisionId());

        ApprovalDocument document =
                documentEligibilityPolicy.getDocumentOrThrow(command.revisionId(), command.documentId());
        FileVersionSummary fileVersion = fileCatalogPort.findFileVersion(document.getFileVersionId()).orElse(null);
        String documentLabel = (fileVersion != null && fileVersion.fileName() != null)
                ? fileVersion.fileName() : String.valueOf(document.getFileVersionId());
        approvalRepository.deleteDocument(command.documentId());

        publishActivity(ActivityLogAction.DELETE, approval.getBlockId(), document.getDocumentId(),
                resourceName(documentLabel), command.requesterId(),
                List.of(new ActivityFieldChange(null, null, null)));

        log.info("결재 문서 제거 완료 - documentId={}", command.documentId());
    }

    @Override
    public ApprovalSubmissionResult submit(SubmitApprovalCommand command) {
        log.info("결재 상신 요청 - approvalId={}, revisionId={}, requesterId={}",
                command.approvalId(), command.revisionId(), command.requesterId());

        Approval approval = revisionEligibilityPolicy.getApprovalOrThrow(command.approvalId());
        revisionEligibilityPolicy.assertDrafter(approval, command.requesterId());
        // 잠금 조회 — 이 락이 트랜잭션 커밋까지 유지되므로 아래 전이 쿼리들은 별도 조건 없이 안전하다(INV-07)
        ApprovalRevision revision =
                revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(command.approvalId(), command.revisionId());

        // SUB-001 — 상신 시 제목·내용·문서·결재선 유효성을 전부 재검증한다(저장 시점 검증과 별개)
        if (isBlank(revision.getTitle()) || isBlank(revision.getContent())) {
            throw new ValidationException(ApprovalErrorCode.APPROVAL_CONTENT_REQUIRED);
        }

        List<ApprovalDocument> documents = approvalRepository.findDocumentsByRevisionId(command.revisionId());
        if (documents.isEmpty()) {
            throw new ValidationException(ApprovalErrorCode.APPROVAL_DOCUMENT_REQUIRED);
        }

        List<ApprovalLine> lines = approvalRepository.findLinesByRevisionId(command.revisionId());
        lineEligibilityPolicy.assertNotEmpty(lines);
        lineEligibilityPolicy.assertOrderValid(lines.stream().map(ApprovalLine::getSequenceNo).toList());
        lineEligibilityPolicy.assertApproversEligible(
                approval.getBlockId(), lines.stream().map(ApprovalLine::getApproverId).toList());

        // SUB-002 — 상태 전이: revision IN_PROGRESS, approval IN_PROGRESS(+current_revision_no), 결재선 ACTIVE/WAITING
        ApprovalRevision submittedRevision = approvalRepository.markRevisionSubmitted(command.revisionId());
        publishActivity(ActivityLogAction.MODIFY, approval.getBlockId(), command.revisionId(),
                resourceName(submittedRevision.getTitle()), command.requesterId(),
                List.of(new ActivityFieldChange(
                        "status", revision.getStatus().name(), submittedRevision.getStatus().name())));
        approvalRepository.markApprovalInProgress(command.approvalId(), submittedRevision.getRevisionNo());
        List<ApprovalLine> activatedLines = approvalRepository.activateLines(command.revisionId());
        Long firstActiveLineId = activatedLines.get(0).getLineId();

        // SUB-003 — 첫 ACTIVE 결재자(firstActiveLineId 의 approverId)에게 알림 이벤트 발행
        domainEventPublisher.publish(NotificationRequestedEvent.of(
                activatedLines.get(0).getApproverId(),
                "APPROVAL_REQUESTED",
                "결재 요청",
                submittedRevision.getTitle() + " 결재 요청이 도착했습니다.",
                approval.getBlockId()));

        log.info("결재 상신 완료 - approvalId={}, revisionId={}, firstActiveLineId={}",
                command.approvalId(), command.revisionId(), firstActiveLineId);

        return new ApprovalSubmissionResult(command.approvalId(), command.revisionId(),
                submittedRevision.getRevisionNo(), submittedRevision.getStatus(),
                submittedRevision.getSubmittedAt(), firstActiveLineId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** changes 가 비어 있으면 발행 안 함(생성자가 빈 리스트를 거부하기도 하고, 실제 변경이 없으면 로그도 없어야 함) */
    private void publishActivity(ActivityLogAction action, Long blockId, Long resourceId, String resourceName,
                                 String actorId, List<ActivityFieldChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        domainEventPublisher.publish(
                ActivityOccurredEvent.of(action, blockId, resourceId, resourceName, actorId, changes));
    }

    private String resourceName(String value) {
        return isBlank(value) ? null : value;
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
