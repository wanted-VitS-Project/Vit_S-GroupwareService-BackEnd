package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.approval.application.command.AddApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ApproveApprovalLineCommand;
import com.group3.vitamins.approval.application.command.RejectApprovalLineCommand;
import com.group3.vitamins.approval.application.command.RemoveApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.SubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalRevisionCommand;
import com.group3.vitamins.approval.application.policy.ApprovalDocumentEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineProcessingPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalLineProcessResult;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    /** 알림 이동 대상 유형(NOTI-V1 GEN-005). 알림 도메인이 아니라 <b>결재가 자기 이름을 정한다</b> */
    private static final String NOTIFICATION_TARGET_TYPE = "APPROVAL";

    private final ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    private final ApprovalLineEligibilityPolicy lineEligibilityPolicy;
    private final ApprovalLineProcessingPolicy lineProcessingPolicy;
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
                .map(line -> approverDisplayName(line.getApproverId()))
                .collect(Collectors.joining(","));

        List<NewApprovalLine> newLines = command.lines().stream()
                .map(input -> new NewApprovalLine(input.approverId(), input.order()))
                .toList();
        List<ApprovalLine> savedLines = approvalRepository.replaceLines(command.revisionId(), newLines);

        // 중복 approverId가 섞여 있어도(순번 검증은 order만 본다) toMap이 IllegalStateException으로 죽지 않게
        // 병합 함수로 방어한다 — 같은 사번이면 이름도 같으니 아무 쪽을 남겨도 결과는 같다.
        Map<String, String> newApproverNames = employees.stream()
                .collect(Collectors.toMap(EmployeeSummary::userId, EmployeeSummary::name, (a, b) -> a));
        String newLineLabel = command.lines().stream()
                .sorted(Comparator.comparingInt(UpdateApprovalLinesCommand.LineInput::order))
                .map(input -> newApproverNames.getOrDefault(input.approverId(), input.approverId()))
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
                        .orElse(new EmployeeSummary(line.getApproverId(), null, null, null, null, null)))
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
                        .orElse(new EmployeeSummary(line.getApproverId(), null, null, null, null, null)))
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
        publishActivity(ActivityLogAction.CREATE, approval.getBlockId(), saved.getFileVersionId(),
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

        publishActivity(ActivityLogAction.DELETE, approval.getBlockId(), document.getFileVersionId(),
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
        publishApprovalNotification(activatedLines.get(0).getApproverId(), "APPROVAL_REQUESTED", "결재 요청",
                submittedRevision.getTitle() + " 결재 요청이 도착했습니다.",
                approval.getApprovalId(), submittedRevision.getRevisionId());

        log.info("결재 상신 완료 - approvalId={}, revisionId={}, firstActiveLineId={}",
                command.approvalId(), command.revisionId(), firstActiveLineId);

        return new ApprovalSubmissionResult(command.approvalId(), command.revisionId(),
                submittedRevision.getRevisionNo(), submittedRevision.getStatus(),
                submittedRevision.getSubmittedAt(), firstActiveLineId);
    }

    @Override
    public ApprovalLineProcessResult approve(ApproveApprovalLineCommand command) {
        log.info("결재 승인 요청 - lineId={}, requesterId={}", command.lineId(), command.requesterId());

        ApprovalLine line = lineProcessingPolicy.getActiveOwnedLineOrThrow(command.lineId(), command.requesterId());
        ApprovalRevision revision = approvalRepository.findRevisionById(line.getRevisionId())
                .orElseThrow(() -> new IllegalStateException("revision not found for line " + command.lineId()));
        Approval approval = approvalRepository.findApproval(revision.getApprovalId())
                .orElseThrow(() -> new IllegalStateException("approval not found for revision " + revision.getRevisionId()));

        ApprovalLine approvedLine = approvalRepository.markLineProcessed(
                command.lineId(), ApprovalLineStatus.APPROVED, command.opinion());

        // PRC-001 — 사용자가 직접 한 행동(라인 자체의 ACTIVE→APPROVED)만 로그. 다음 라인 활성화·회차/결재
        // 완료 전환은 파생 효과라 기록 안 함(상신#7과 동일 원칙)
        publishActivity(ActivityLogAction.MODIFY, approval.getBlockId(), approvedLine.getLineId(),
                resourceName(revision.getTitle()), command.requesterId(),
                List.of(new ActivityFieldChange("status",
                        ApprovalLineStatus.ACTIVE.name(), ApprovalLineStatus.APPROVED.name())));

        Optional<ApprovalLine> nextLine =
                approvalRepository.findLineBySequenceNo(line.getRevisionId(), line.getSequenceNo() + 1);
        Long nextActiveLineId = null;
        boolean approvalCompleted = false;

        if (nextLine.isPresent()) {
            // PRC-002 — 다음 결재선 활성화 + 그 결재자에게 요청 알림(SUB-003과 동일 패턴)
            ApprovalLine activated = approvalRepository.activateLine(nextLine.get().getLineId());
            nextActiveLineId = activated.getLineId();
            publishApprovalNotification(activated.getApproverId(), "APPROVAL_REQUESTED", "결재 요청",
                    revision.getTitle() + " 결재 요청이 도착했습니다.", approval.getApprovalId(), revision.getRevisionId());
        } else {
            // PRC-002 — 마지막 순번 승인 → 회차·결재 모두 COMPLETED 종료 + 기안자에게 완료 알림
            approvalRepository.finalizeApproval(approval.getApprovalId(), revision.getRevisionId(), ApprovalStatus.COMPLETED);
            approvalCompleted = true;
            publishApprovalNotification(approval.getDrafterId(), "APPROVAL_COMPLETED", "결재 완료",
                    revision.getTitle() + " 결재가 완료되었습니다.", approval.getApprovalId(), revision.getRevisionId());
        }

        log.info("결재 승인 완료 - lineId={}, nextActiveLineId={}, approvalCompleted={}",
                command.lineId(), nextActiveLineId, approvalCompleted);

        return new ApprovalLineProcessResult(approvedLine.getLineId(), approvedLine.getStatus().name(),
                approvedLine.getProcessedAt(), nextActiveLineId, approvalCompleted);
    }

    @Override
    public ApprovalLineProcessResult reject(RejectApprovalLineCommand command) {
        log.info("결재 반려 요청 - lineId={}, requesterId={}", command.lineId(), command.requesterId());

        ApprovalLine line = lineProcessingPolicy.getActiveOwnedLineOrThrow(command.lineId(), command.requesterId());
        ApprovalRevision revision = approvalRepository.findRevisionById(line.getRevisionId())
                .orElseThrow(() -> new IllegalStateException("revision not found for line " + command.lineId()));
        Approval approval = approvalRepository.findApproval(revision.getApprovalId())
                .orElseThrow(() -> new IllegalStateException("approval not found for revision " + revision.getRevisionId()));

        ApprovalLine rejectedLine = approvalRepository.markLineProcessed(
                command.lineId(), ApprovalLineStatus.REJECTED, command.opinion());

        // PRC-005 — 사용자가 직접 한 행동(라인 자체의 ACTIVE→REJECTED)만 로그. 다운스트림 CANCELED·
        // 회차/결재 REJECTED 전환은 파생 효과라 기록 안 함(승인#11과 동일 원칙)
        publishActivity(ActivityLogAction.MODIFY, approval.getBlockId(), rejectedLine.getLineId(),
                resourceName(revision.getTitle()), command.requesterId(),
                List.of(new ActivityFieldChange("status",
                        ApprovalLineStatus.ACTIVE.name(), ApprovalLineStatus.REJECTED.name())));

        // PRC-007 — 이후 WAITING 단계 전부 CANCELED, 회차·결재 전체 REJECTED로 종료
        approvalRepository.cancelWaitingLinesAfter(line.getRevisionId(), line.getSequenceNo());
        approvalRepository.finalizeApproval(approval.getApprovalId(), revision.getRevisionId(), ApprovalStatus.REJECTED);

        // PRC-008 — 기안자에게만 반려 알림
        publishApprovalNotification(approval.getDrafterId(), "APPROVAL_REJECTED", "결재 반려",
                revision.getTitle() + " 결재가 반려되었습니다.", approval.getApprovalId(), revision.getRevisionId());

        log.info("결재 반려 완료 - lineId={}", command.lineId());

        return new ApprovalLineProcessResult(rejectedLine.getLineId(), rejectedLine.getStatus().name(),
                rejectedLine.getProcessedAt(), null, true);
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

    /**
     * 결재 알림 발행 공통부(SUB-003 · PRC-002 · PRC-008).
     *
     * <p>이동 대상을 결재가 직접 지정한다(NOTI-V1 GEN-005). {@code revisionId} 는 <b>알림 생성 시점 값을
     * 스냅샷</b>으로 넘긴다(VIW-010) — 상신·승인·반려마다 각각 별도 알림이 생기므로, 각 알림은 그 사건
     * 당시의 회차를 가리켜야 메시지와 목적지가 일치한다. 클릭 시점에 최신 회차를 다시 찾지 않는다.
     */
    private void publishApprovalNotification(String recipientUserId, String notificationType, String title,
                                             String message, Long approvalId, Long revisionId) {
        domainEventPublisher.publish(NotificationRequestedEvent.of(
                recipientUserId, notificationType, title, message,
                NOTIFICATION_TARGET_TYPE, approvalId, Map.of("revisionId", revisionId)));
    }

    private String approverDisplayName(String approverId) {
        // 이름 조회는 활동 로그 표시용 부가 정보다 — 조회 자체가 실패(타임아웃 등)해도
        // 결재선 교체(핵심 요청)는 막지 않고 사번으로 대체한다.
        try {
            return employeeCatalogPort.findEmployee(approverId)
                    .map(EmployeeSummary::name)
                    .orElse(approverId);
        } catch (RuntimeException e) {
            log.warn("결재자 이름 조회 실패 - approverId={}, 사번으로 대체", approverId, e);
            return approverId;
        }
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
