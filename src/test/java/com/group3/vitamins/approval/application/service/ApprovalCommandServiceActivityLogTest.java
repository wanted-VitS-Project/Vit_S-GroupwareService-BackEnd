package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.approval.application.command.AddApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.policy.ApprovalDocumentEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalCommandServiceActivityLogTest {

    @Mock
    private ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    @Mock
    private ApprovalLineEligibilityPolicy lineEligibilityPolicy;
    @Mock
    private ApprovalDocumentEligibilityPolicy documentEligibilityPolicy;
    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private FileCatalogPort fileCatalogPort;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ApprovalCommandService service;

    @Test
    void documentCreateUsesFileVersionAndNameWithNullChange() {
        Approval approval = approval(10L);
        FileVersionSummary file = new FileVersionSummary(300L, "COMPLETED", "품의서.pdf", 10L, null);
        ApprovalDocument document = ApprovalDocument.reconstruct(400L, 200L, 300L, null);

        when(revisionEligibilityPolicy.getApprovalOrThrow(100L)).thenReturn(approval);
        when(documentEligibilityPolicy.getReadyFileVersionOrThrow(300L)).thenReturn(file);
        when(approvalRepository.addDocument(200L, 300L)).thenReturn(document);

        service.addDocument(new AddApprovalDocumentCommand(100L, 200L, 300L, "EMP001"));

        ActivityOccurredEvent event = capturedActivityEvent();
        assertThat(event.action()).isEqualTo(ActivityLogAction.CREATE);
        assertThat(event.blockId()).isEqualTo(10L);
        assertThat(event.resourceId()).isEqualTo(300L);
        assertThat(event.resourceName()).isEqualTo("품의서.pdf");
        assertThat(event.actorId()).isEqualTo("EMP001");
        assertThat(event.changes()).singleElement().satisfies(change -> {
            assertThat(change.field()).isNull();
            assertThat(change.beforeValue()).isNull();
            assertThat(change.afterValue()).isNull();
        });
    }

    @Test
    void lineOrderChangeIsRecordedInApprovalOrder() {
        Approval approval = approval(10L);
        ApprovalRevision revision = ApprovalRevision.reconstruct(
                200L, 100L, 1, "휴가 품의", "내용", ApprovalStatus.DRAFT,
                null, null, null, null, null);
        ApprovalLine first = line(1L, "EMP001", 1);
        ApprovalLine second = line(2L, "EMP002", 2);
        UpdateApprovalLinesCommand command = new UpdateApprovalLinesCommand(
                100L, 200L, "EMP000",
                List.of(
                        new UpdateApprovalLinesCommand.LineInput("EMP001", 2),
                        new UpdateApprovalLinesCommand.LineInput("EMP002", 1)));

        when(revisionEligibilityPolicy.getApprovalOrThrow(100L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(100L, 200L)).thenReturn(revision);
        when(lineEligibilityPolicy.assertApproversEligible(10L, List.of("EMP001", "EMP002")))
                .thenReturn(List.of(employee("EMP001", "김철수"), employee("EMP002", "이영희")));
        when(employeeCatalogPort.findEmployee("EMP001"))
                .thenReturn(java.util.Optional.of(employee("EMP001", "김철수")));
        when(employeeCatalogPort.findEmployee("EMP002"))
                .thenReturn(java.util.Optional.of(employee("EMP002", "이영희")));
        when(approvalRepository.findLinesByRevisionId(200L)).thenReturn(List.of(first, second));
        when(approvalRepository.replaceLines(org.mockito.ArgumentMatchers.eq(200L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(second, first));

        service.updateLines(command);

        ActivityOccurredEvent event = capturedActivityEvent();
        assertThat(event.action()).isEqualTo(ActivityLogAction.MODIFY);
        assertThat(event.resourceId()).isEqualTo(200L);
        assertThat(event.resourceName()).isEqualTo("휴가 품의");
        assertThat(event.changes()).singleElement().satisfies(change -> {
            assertThat(change.field()).isEqualTo("lines");
            assertThat(change.beforeValue()).isEqualTo("김철수,이영희");
            assertThat(change.afterValue()).isEqualTo("이영희,김철수");
        });
    }

    @Test
    void lineOrderChangeFallsBackToApproverIdWhenEmployeeLookupMisses() {
        Approval approval = approval(10L);
        ApprovalRevision revision = ApprovalRevision.reconstruct(
                200L, 100L, 1, "휴가 품의", "내용", ApprovalStatus.DRAFT,
                null, null, null, null, null);
        ApprovalLine previousLine = line(1L, "EMP009", 1);
        ApprovalLine savedLine = line(2L, "EMP001", 1);
        UpdateApprovalLinesCommand command = new UpdateApprovalLinesCommand(
                100L, 200L, "EMP000",
                List.of(new UpdateApprovalLinesCommand.LineInput("EMP001", 1)));

        when(revisionEligibilityPolicy.getApprovalOrThrow(100L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(100L, 200L)).thenReturn(revision);
        when(lineEligibilityPolicy.assertApproversEligible(10L, List.of("EMP001")))
                .thenReturn(List.of(employee("EMP001", "김철수")));
        // "EMP009"(퇴사·탈퇴 등으로 조회 불가)는 스텁하지 않아 findEmployee가 기본값 Optional.empty()를 반환한다.
        when(approvalRepository.findLinesByRevisionId(200L)).thenReturn(List.of(previousLine));
        // replaceLines는 실제로 저장된 새 결재선(EMP001)을 돌려줘야 employees와 zip 결과도 맞다.
        when(approvalRepository.replaceLines(org.mockito.ArgumentMatchers.eq(200L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(savedLine));

        List<com.group3.vitamins.approval.application.result.ApprovalLineView> result = service.updateLines(command);

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.approverId()).isEqualTo("EMP001");
            assertThat(view.approverName()).isEqualTo("김철수");
        });

        ActivityOccurredEvent event = capturedActivityEvent();
        assertThat(event.changes()).singleElement().satisfies(change -> {
            assertThat(change.field()).isEqualTo("lines");
            assertThat(change.beforeValue()).isEqualTo("EMP009");
            assertThat(change.afterValue()).isEqualTo("김철수");
        });
    }

    @Test
    void lineOrderUnchangedDoesNotPublishActivity() {
        Approval approval = approval(10L);
        ApprovalRevision revision = ApprovalRevision.reconstruct(
                200L, 100L, 1, "휴가 품의", "내용", ApprovalStatus.DRAFT,
                null, null, null, null, null);
        ApprovalLine existingLine = line(1L, "EMP001", 1);
        UpdateApprovalLinesCommand command = new UpdateApprovalLinesCommand(
                100L, 200L, "EMP000",
                List.of(new UpdateApprovalLinesCommand.LineInput("EMP001", 1)));

        when(revisionEligibilityPolicy.getApprovalOrThrow(100L)).thenReturn(approval);
        when(revisionEligibilityPolicy.getDraftRevisionForUpdateOrThrow(100L, 200L)).thenReturn(revision);
        when(lineEligibilityPolicy.assertApproversEligible(10L, List.of("EMP001")))
                .thenReturn(List.of(employee("EMP001", "김철수")));
        when(employeeCatalogPort.findEmployee("EMP001"))
                .thenReturn(java.util.Optional.of(employee("EMP001", "김철수")));
        when(approvalRepository.findLinesByRevisionId(200L)).thenReturn(List.of(existingLine));
        when(approvalRepository.replaceLines(org.mockito.ArgumentMatchers.eq(200L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(existingLine));

        service.updateLines(command);

        org.mockito.Mockito.verifyNoInteractions(domainEventPublisher);
    }

    private ActivityOccurredEvent capturedActivityEvent() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        return (ActivityOccurredEvent) captor.getValue();
    }

    private Approval approval(Long blockId) {
        return Approval.reconstruct(100L, blockId, "EMP001", ApprovalStatus.DRAFT,
                1, null, null, null, null);
    }

    private ApprovalLine line(Long lineId, String approverId, int sequenceNo) {
        return ApprovalLine.reconstruct(lineId, 200L, approverId, sequenceNo,
                ApprovalLineStatus.DRAFT, null, null, null, null);
    }

    private EmployeeSummary employee(String userId, String name) {
        return new EmployeeSummary(userId, name, null, null, null, null);
    }
}
